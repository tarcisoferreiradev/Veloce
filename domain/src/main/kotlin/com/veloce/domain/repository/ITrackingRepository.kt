package com.veloce.domain.repository

import com.veloce.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ITrackingRepository {
    suspend fun createSession(session: TrackingSession): Outcome<Unit>
    suspend fun append(point: LocationPoint, elapsedMillis: Long): Outcome<Unit>
    suspend fun finishSession(id: String, endedAt: Long, elapsedMillis: Long): Outcome<Unit>
    fun observeLatestSession(): Flow<Outcome<TrackingSession?>>
    fun observeAllSessions(): Flow<Outcome<List<TrackingSession>>>
    fun observeSession(sessionId: String): Flow<Outcome<TrackingSession?>>
    fun observePoints(sessionId: String): Flow<Outcome<List<LocationPoint>>>
    suspend fun getPoints(sessionId: String): Outcome<List<LocationPoint>>
}

interface LocationSource {
    fun points(sessionId: String): Flow<Outcome<LocationPoint>>
}
