package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.ConnectPost
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.time.Instant

data class ConnectCreatePostUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class ConnectCreatePostViewModel(
    private val repository: PuneConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectCreatePostUiState())
    val uiState: StateFlow<ConnectCreatePostUiState> = _uiState.asStateFlow()

    fun createPost(
        title: String,
        description: String,
        category: String,
        area: String,
        imageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
             val userId = SupabaseClient.getSessionManager().getUserId() ?: run {
                 _uiState.value = _uiState.value.copy(error = "User not logged in")
                 return@launch
             }

             _uiState.value = _uiState.value.copy(isLoading = true, error = null)

             var imageUrl: String? = null
             if (imageBytes != null) {
                 val fileName = "${UUID.randomUUID()}.jpg"
                 repository.uploadImage(fileName, imageBytes).onSuccess {
                     imageUrl = it
                 }.onFailure { e ->
                     _uiState.value = _uiState.value.copy(isLoading = false, error = "Image upload failed: ${e.message}")
                     return@launch
                 }
             }

             val newPost = ConnectPost(
                 id = UUID.randomUUID().toString(),
                 userId = userId,
                 title = title,
                 description = description,
                 category = category,
                 area = area,
                 imageUrl = imageUrl,
                 upvotes = 0,
                 downvotes = 0,
                 createdAt = null // Let Supabase set now() on the server
             )

             repository.createPost(newPost).onSuccess {
                 _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
             }.onFailure { e ->
                 _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
             }
        }
    }

    companion object {
        fun factory(repository: PuneConnectRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConnectCreatePostViewModel(repository) as T
                }
            }
        }
    }
}
