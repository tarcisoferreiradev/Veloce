package com.veloce.domain.tracking

import com.veloce.domain.model.*
import com.veloce.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

interface TrackingClock {
    fun wallMillis(): Long
    fun monotonicMillis(): Long
}

private class TrackingException(val failure: TrackingFailure) : Exception()

/** One engine per process, run by the foreground service, never by an Activity. */
class TrackingEngine(
    private val repository: ITrackingRepository,
    private val location: LocationSource,
    private val clock: TrackingClock,
    private val newId: () -> String,
) {
    private val mutableStatus = MutableStateFlow(TrackingStatus())
    val status = mutableStatus.asStateFlow()

    fun report(failure: TrackingFailure) { mutableStatus.update { it.copy(failure = failure) } }

    suspend fun run() {
        if (status.value.running || status.value.busy) return
        mutableStatus.value = TrackingStatus(busy = true)
        var sessionId: String? = null
        var startMonotonic: Long? = null
        try {
            closeInterruptedSession()
            val id = newId()
            repository.createSession(TrackingSession(id, clock.wallMillis())).requireSuccess()
            sessionId = id
            startMonotonic = clock.monotonicMillis()
            mutableStatus.value = TrackingStatus(running = true, sessionId = id)
            capture(id, startMonotonic)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: TrackingException) { report(error.failure)
        } catch (_: Exception) { report(TrackingFailure.Unexpected)
        } finally {
            mutableStatus.update { it.copy(running = false, busy = true) }
            withContext(NonCancellable) {
                if (sessionId != null && startMonotonic != null) {
                    val elapsed = elapsedSince(startMonotonic)
                    val result = repository.finishSession(sessionId, clock.wallMillis(), elapsed)
                    mutableStatus.update { it.copy(elapsedMillis = elapsed) }
                    if (result is Outcome.Failure) report(result.error)
                }
            }
            mutableStatus.update { it.copy(busy = false) }
        }
    }

    private suspend fun capture(id: String, started: Long): Unit = coroutineScope {
        val timer = launch {
            while (isActive) {
                mutableStatus.update { it.copy(elapsedMillis = elapsedSince(started)) }
                delay(1_000)
            }
        }
        try {
            location.points(id).collect { result ->
                val point = result.requireSuccess()
                repository.append(point, elapsedSince(started)).requireSuccess()
            }
        } finally { timer.cancel() }
    }

    private suspend fun closeInterruptedSession() {
        val session = repository.observeLatestSession().first().requireSuccess()
        if (session != null && session.endedAt == null) {
            // A killed process cannot prove time or route after the last committed checkpoint.
            repository.finishSession(session.id, session.startedAt + session.elapsedMillis,
                session.elapsedMillis).requireSuccess()
        }
    }
    private fun elapsedSince(start: Long) = (clock.monotonicMillis() - start).coerceAtLeast(0)
}

private fun <T> Outcome<T>.requireSuccess(): T = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> throw TrackingException(error)
}
