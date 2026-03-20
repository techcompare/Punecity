package com.pranav.punecityguide.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pranav.punecityguide.data.model.Itinerary
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.ItineraryRepository
import com.pranav.punecityguide.domain.scantoplan.AttractionMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ScanToPlanUiState(
    val isWorking: Boolean = false,
    val recognizedText: String = "",
    val matchedAttractionName: String? = null,
    val matchedAttractionId: Int? = null,
    val matchedAttraction: com.pranav.punecityguide.data.model.Attraction? = null,
    val capturedBitmap: android.graphics.Bitmap? = null,
    val savedItineraryId: Int? = null,
    val error: String? = null,
)

class ScanToPlanViewModel(
    private val attractionRepository: AttractionRepository,
    private val itineraryRepository: ItineraryRepository,
    private val auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanToPlanUiState())
    val uiState: StateFlow<ScanToPlanUiState> = _uiState.asStateFlow()

    private val TAG = "ScanToPlanVM"

    /**
     * Outstanding Feature: Dual-Language Scan
     * Uses both Devanagari (Marathi/Hindi) and Latin (English) OCR.
     */
    fun scan(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isWorking = true, 
                    error = null, 
                    savedItineraryId = null,
                    recognizedText = "",
                    matchedAttractionName = null,
                    matchedAttraction = null,
                    capturedBitmap = bitmap
                )

                // Initialize recognizers
                val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                
                val image = InputImage.fromBitmap(bitmap, 0)

                Log.d(TAG, "Starting dual-language scan (Latin + Devanagari)")
                
                // Process both in parallel or sequence
                val latinResult = latinRecognizer.process(image).await()
                val devResult = devanagariRecognizer.process(image).await()
                
                val combinedText = (latinResult.text + "\n" + devResult.text).trim()
                
                if (combinedText.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        error = "No text recognized. Try a clearer photo of a sign or ticket."
                    )
                    return@launch
                }

                auditRepository.log("SCAN_COMPLETE", "Recognized ${combinedText.length} chars (Dual Mode)")

                val attractions = attractionRepository.getAllAttractions()
                val match = AttractionMatcher.bestMatch(attractions, combinedText)
                
                val fullAttraction = match?.let { attractionRepository.getAttractionById(it.attraction.id) }

                if (fullAttraction != null) {
                    auditRepository.log("SCAN_MATCH_SUCCESS", "Matched to ${fullAttraction.name}")
                }

                _uiState.value = _uiState.value.copy(
                    recognizedText = combinedText,
                    matchedAttractionName = fullAttraction?.name,
                    matchedAttractionId = fullAttraction?.id,
                    matchedAttraction = fullAttraction,
                    isWorking = false,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    error = "Scan failed: ${e.localizedMessage ?: "Try again with a better photo"}",
                )
            }
        }
    }

    fun savePlan(title: String, notes: String = "") {
        val state = _uiState.value
        if (state.recognizedText.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isWorking = true, error = null)
                val id = itineraryRepository.insert(
                    Itinerary(
                        title = title.ifBlank { "Scan Plan" },
                        createdAtEpochMillis = System.currentTimeMillis(),
                        sourceText = state.recognizedText,
                        matchedAttractionId = state.matchedAttractionId,
                        notes = notes,
                    )
                ).toInt()
                auditRepository.log("ITINERARY_SCAN_SAVE", "Saved scan result to ID: $id")
                _uiState.value = _uiState.value.copy(isWorking = false, savedItineraryId = id)
            } catch (e: Exception) {
                auditRepository.log("ITINERARY_SAVE_FAILURE", "Save failed: ${e.message}")
                _uiState.value = _uiState.value.copy(isWorking = false, error = e.message ?: "Save failed")
            }
        }
    }

    companion object {
        fun factory(
            attractionRepository: AttractionRepository,
            itineraryRepository: ItineraryRepository,
            auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScanToPlanViewModel(attractionRepository, itineraryRepository, auditRepository) as T
                }
            }
        }
    }

    fun handlePermissionDenied(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
}
