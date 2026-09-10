package com.veloce.domain.usecase

import com.veloce.core.math.haversineMeters
import com.veloce.core.math.paceMinutesPerKm
import com.veloce.domain.model.Outcome
import com.veloce.domain.model.TrackingSession
import com.veloce.domain.repository.ITrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class HistorySession(
    val session: TrackingSession,
    val distanceMeters: Double,
    val pace: Double?
)

class GetHistorySessionsUseCase(
    private val repository: ITrackingRepository
) {
    operator fun invoke(): Flow<Outcome<List<HistorySession>>> {
        return repository.observeAllSessions().map { outcome ->
            when (outcome) {
                is Outcome.Success -> {
                    val sessions = outcome.value.map { session ->
                        val pointsResult = repository.getPoints(session.id)
                        var distance = 0.0
                        if (pointsResult is Outcome.Success) {
                            val points = pointsResult.value
                            for (i in 1 until points.size) {
                                val p1 = points[i - 1]
                                val p2 = points[i]
                                distance += haversineMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                            }
                        }
                        val durationSec = session.elapsedMillis / 1000.0
                        val pace = paceMinutesPerKm(distance, durationSec)
                        HistorySession(session, distance, pace)
                    }
                    Outcome.Success(sessions)
                }
                is Outcome.Failure -> Outcome.Failure(outcome.error)
            }
        }
    }
}
