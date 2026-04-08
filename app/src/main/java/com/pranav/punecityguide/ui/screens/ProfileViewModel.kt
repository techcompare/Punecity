package com.pranav.punecityguide.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.ProfileRepository
import com.pranav.punecityguide.model.ProfileData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: ProfileData? = null,
    val error: String? = null,
    val postsCount: Int = 0,
    val savedCount: Int = 0,
    val visitsCount: Int = 0,
    val xp: Int = 0,
    val isEditing: Boolean = false,
    val updateSuccess: String? = null,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val prefs = application.getSharedPreferences("punebuzz_prefs", Context.MODE_PRIVATE)

    init {
        loadLocalProfile()
    }
    
    private fun loadLocalProfile() {
        // Load from local storage first (instant)
        val sessionMode = prefs.getString("session_mode", "none") ?: "none"
        val sessionName = prefs.getString("session_name", "Pune Explorer") ?: "Pune Explorer"
        val sessionEmail = prefs.getString("session_email", null)
        val profilePhotoUri = prefs.getString("profile_photo_uri", null)
        val postsCount = prefs.getInt("user_posts_count", 0)
        val savedCount = prefs.getInt("user_saved_count", 0)
        val visitsCount = prefs.getInt("user_visits_count", 0)
        val xp = prefs.getInt("user_xp", 0)
        
        val localProfile = ProfileData(
            displayName = sessionName,
            username = sessionEmail?.substringBefore("@"),
            email = sessionEmail,
            bio = null,
            profilePhotoUri = profilePhotoUri
        )
        
        _uiState.update { 
            it.copy(
                isLoading = false, 
                profile = if (sessionMode != "none") localProfile else null,
                postsCount = postsCount,
                savedCount = savedCount,
                visitsCount = visitsCount,
                xp = xp,
                error = null
            ) 
        }
        
        // Then try to fetch from server (optional enhancement)
        if (sessionMode == "auth") {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            ProfileRepository.getProfile().onSuccess { profile ->
                if (profile != null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            profile = profile, 
                            error = null
                        ) 
                    }
                } else {
                    // Keep local profile if server returns null
                    _uiState.update { it.copy(isLoading = false) }
                }
            }.onFailure { error ->
                // Don't show error, just keep local profile
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun updateDisplayName(newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.length < 2) {
            _uiState.update { it.copy(error = "Name must be at least 2 characters") }
            return
        }
        if (trimmedName.length > 30) {
            _uiState.update { it.copy(error = "Name must be 30 characters or less") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isEditing = true, error = null) }
            
            // Save locally
            prefs.edit().putString("session_name", trimmedName).apply()
            
            // Update UI state
            val currentProfile = _uiState.value.profile
            val updatedProfile = currentProfile?.copy(displayName = trimmedName) ?: ProfileData(
                displayName = trimmedName,
                username = null,
                email = null,
                bio = null
            )
            
            _uiState.update { 
                it.copy(
                    isEditing = false, 
                    profile = updatedProfile,
                    updateSuccess = "Name updated successfully"
                ) 
            }
        }
    }
    
    fun updateProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditing = true, error = null) }
            
            try {
                // Take persistent URI permission for the selected image
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Permission might not be available for this URI, continue anyway
            }
            
            // Save URI locally
            val uriString = uri.toString()
            prefs.edit().putString("profile_photo_uri", uriString).apply()
            
            // Update UI state
            val currentProfile = _uiState.value.profile
            val updatedProfile = currentProfile?.copy(profilePhotoUri = uriString) ?: ProfileData(
                displayName = "Pune Explorer",
                username = null,
                email = null,
                bio = null,
                profilePhotoUri = uriString
            )
            
            _uiState.update { 
                it.copy(
                    isEditing = false, 
                    profile = updatedProfile,
                    updateSuccess = "Photo updated successfully"
                ) 
            }
        }
    }
    
    fun removeProfilePhoto() {
        viewModelScope.launch {
            prefs.edit().remove("profile_photo_uri").apply()
            
            val currentProfile = _uiState.value.profile
            val updatedProfile = currentProfile?.copy(profilePhotoUri = null)
            
            _uiState.update { 
                it.copy(
                    profile = updatedProfile,
                    updateSuccess = "Photo removed"
                ) 
            }
        }
    }
    
    fun clearUpdateSuccess() {
        _uiState.update { it.copy(updateSuccess = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun incrementPostsCount() {
        val newCount = _uiState.value.postsCount + 1
        val newXp = _uiState.value.xp + 10
        prefs.edit()
            .putInt("user_posts_count", newCount)
            .putInt("user_xp", newXp)
            .apply()
        _uiState.update { it.copy(postsCount = newCount, xp = newXp) }
    }
    
    fun incrementSavedCount() {
        val newCount = _uiState.value.savedCount + 1
        val newXp = _uiState.value.xp + 5
        prefs.edit()
            .putInt("user_saved_count", newCount)
            .putInt("user_xp", newXp)
            .apply()
        _uiState.update { it.copy(savedCount = newCount, xp = newXp) }
    }
}
