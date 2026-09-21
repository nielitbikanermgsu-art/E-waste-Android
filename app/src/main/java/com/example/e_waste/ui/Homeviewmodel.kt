package com.example.e_waste.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elink.app.data.repository.EwasteRepository
import com.elink.app.model.LotSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(val recentLots: List<LotSummary> = emptyList())

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EwasteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Room-backed Flow — reflects local cache immediately, then updates as background
        // sync (WorkManager) brings in anything created from another device/session.
        viewModelScope.launch {
            repository.recentLots(limit = 5).collect { lots ->
                _uiState.value = HomeUiState(recentLots = lots)
            }
        }
    }
}