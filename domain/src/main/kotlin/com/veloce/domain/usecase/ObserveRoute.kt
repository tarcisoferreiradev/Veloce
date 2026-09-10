package com.veloce.domain.usecase

import com.veloce.core.math.haversineMeters
import com.veloce.domain.model.*
import com.veloce.domain.repository.ITrackingRepository

data class Route(val points: List<LocationPoint> = emptyList(), val distanceMeters: Double = 0.0, val sessionId: String? = null)

/** Each collector owns its accumulator; appending computes only new segments. */
class ObserveRoute(private val repository: ITrackingRepository) {
    operator fun invoke(sessionId: String) = kotlinx.coroutines.flow.flow {
        var previous = emptyList<LocationPoint>()
        var distance = 0.0
        repository.observePoints(sessionId).collect { result ->
            when (result) {
                is Outcome.Failure -> emit(result)
                is Outcome.Success -> {
                    val points = result.value
                    val appended = points.size >= previous.size &&
                        points.take(previous.size) == previous
                    val start = if (appended) previous.size.coerceAtLeast(1) else 1
                    if (!appended) distance = 0.0
                    for (index in start until points.size) {
                        val a = points[index - 1]
                        val b = points[index]
                        distance += haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                    }
                    previous = points
                    emit(Outcome.Success(Route(points, distance, sessionId)))
                }
            }
        }
    }
}
