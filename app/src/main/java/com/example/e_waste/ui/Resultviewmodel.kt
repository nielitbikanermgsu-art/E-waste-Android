package com.example.e_waste.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elink.app.data.repository.EwasteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Detection(
    val categoryName: String,
    val confidence: Float,
    // Normalized (0..1) coordinates in the ORIGINAL photo's frame — the server has already
    // mapped these back from its internal 640x640 letterboxed inference space, so this
    // screen never needs to know anything about the model's input size.
    val xMinNorm: Float,
    val yMinNorm: Float,
    val xMaxNorm: Float,
    val yMaxNorm: Float
)

data class MaterialInfo(val name: String, val pricingTier: String) // "primary" | "trace"
data class RecyclerInfo(val name: String, val distanceKm: Double)

sealed interface DetectionUiState {
    data object Idle : DetectionUiState
    data class Uploading(val progress: Float? = null) : DetectionUiState
    data class Success(
        val sessionId: String,
        val detections: List<Detection>,
        val materials: List<MaterialInfo>,
        val recyclers: List<RecyclerInfo>
    ) : DetectionUiState
    data class Error(val message: String, val retryable: Boolean) : DetectionUiState
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: EwasteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetectionUiState>(DetectionUiState.Idle)
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    private var hasStarted = false

    fun detect(photoUri: String) {
        // Guard against LaunchedEffect re-firing on configuration-change recomposition —
        // we only want one upload per screen instance, not one per recomposition.
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            _uiState.value = DetectionUiState.Uploading()
            val result = repository.detect(photoUri)
            _uiState.value = result.fold(
                onSuccess = { response ->
                    DetectionUiState.Success(
                        sessionId = response.sessionId,
                        detections = response.detections,
                        materials = response.materials,
                        recyclers = response.recyclers
                    )
                },
                onFailure = { throwable ->
                    DetectionUiState.Error(
                        message = throwable.message ?: "Something went wrong. Please try again.",
                        retryable = true
                    )
                }
            )
        }
    }
}