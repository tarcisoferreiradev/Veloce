package com.veloce.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.veloce.domain.model.Outcome
import com.veloce.domain.model.LocationPoint
import com.veloce.domain.usecase.ExportFormat
import com.veloce.domain.usecase.ExportSessionUseCase
import com.veloce.domain.usecase.GetHistorySessionsUseCase
import com.veloce.domain.usecase.HistorySession
import com.veloce.domain.repository.ITrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<HistorySession> = emptyList(),
    val error: String? = null,
    val loading: Boolean = true,
    val exportData: String? = null,
    val exportFormat: ExportFormat? = null,
    val exportError: String? = null,
    val sessionPoints: Map<String, List<LocationPoint>> = emptyMap()
)

class HistoryViewModel(
    private val getHistorySessionsUseCase: GetHistorySessionsUseCase,
    private val exportSessionUseCase: ExportSessionUseCase,
    private val trackingRepository: ITrackingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getHistorySessionsUseCase().collect { result ->
                when (result) {
                    is Outcome.Success -> {
                        _state.update { it.copy(sessions = result.value, loading = false, error = null) }
                    }
                    is Outcome.Failure -> {
                        _state.update { it.copy(loading = false, error = "Failed to load history") }
                    }
                }
            }
        }
    }

    fun exportSession(sessionId: String, format: ExportFormat) {
        viewModelScope.launch {
            val result = exportSessionUseCase(sessionId, format)
            when (result) {
                is Outcome.Success -> {
                    _state.update { it.copy(exportData = result.value, exportFormat = format, exportError = null) }
                }
                is Outcome.Failure -> {
                    _state.update { it.copy(exportError = "Failed to export session") }
                }
            }
        }
    }

    fun clearExport() {
        _state.update { it.copy(exportData = null, exportFormat = null, exportError = null) }
    }

    fun loadPoints(sessionId: String) {
        if (_state.value.sessionPoints.containsKey(sessionId)) return
        viewModelScope.launch {
            val result = trackingRepository.getPoints(sessionId)
            if (result is Outcome.Success) {
                _state.update { it.copy(sessionPoints = it.sessionPoints + (sessionId to result.value)) }
            }
        }
    }

    class Factory(
        private val getHistorySessionsUseCase: GetHistorySessionsUseCase,
        private val exportSessionUseCase: ExportSessionUseCase,
        private val trackingRepository: ITrackingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(getHistorySessionsUseCase, exportSessionUseCase, trackingRepository) as T
    }
}
