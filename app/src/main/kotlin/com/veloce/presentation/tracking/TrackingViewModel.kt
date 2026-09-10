package com.veloce.presentation.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.veloce.core.math.paceMinutesPerKm
import com.veloce.domain.model.*
import com.veloce.domain.repository.ITrackingRepository
import com.veloce.domain.tracking.*
import com.veloce.domain.usecase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class TrackingUiState(
    val session: TrackingSession? = null,
    val route: Route = Route(),
    val elapsedSeconds: Long = 0,
    val pace: Double? = null,
    val running: Boolean = false,
    val busy: Boolean = false,
    val interrupted: Boolean = false,
    val failure: TrackingFailure? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModel(repository: ITrackingRepository, private val controller: TrackingController) : ViewModel() {
    private val session = repository.observeLatestSession().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), Outcome.Success(null),
    )
    private val route = session.map { (it as? Outcome.Success)?.value?.id }
        .distinctUntilChanged().flatMapLatest { id ->
            if (id == null) flowOf(Outcome.Success(Route())) else ObserveRoute(repository)(id)
        }
    val state = combine(session, route, controller.status) { sessionResult, routeResult, status ->
        val current = (sessionResult as? Outcome.Success)?.value
        val currentRoute = (routeResult as? Outcome.Success)?.value?.takeIf { it.sessionId == current?.id } ?: Route()
        val elapsed = if (status.sessionId == current?.id && current != null)
            maxOf(status.elapsedMillis, current.elapsedMillis) else current?.elapsedMillis ?: 0
        TrackingUiState(
            session = current, route = currentRoute, elapsedSeconds = elapsed / 1000,
            pace = paceMinutesPerKm(currentRoute.distanceMeters, elapsed / 1000.0),
            running = status.running, busy = status.busy,
            interrupted = current != null && current.endedAt == null && !status.running && !status.busy,
            failure = status.failure ?: (sessionResult as? Outcome.Failure)?.error ?: (routeResult as? Outcome.Failure)?.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackingUiState())

    fun start() = controller.start()
    fun stop() = controller.stop()

    class Factory(private val repository: ITrackingRepository, private val controller: TrackingController) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TrackingViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return TrackingViewModel(repository, controller) as T
        }
    }
}
