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

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: EwasteRepository
) : ViewModel() {

    private val _lots = MutableStateFlow<List<LotSummary>>(emptyList())
    val lots: StateFlow<List<LotSummary>> = _lots.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allLots().collect { _lots.value = it }
        }
    }
}