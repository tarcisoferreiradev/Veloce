package com.veloce.domain

import com.veloce.domain.model.*
import com.veloce.domain.repository.*
import com.veloce.domain.tracking.*
import com.veloce.domain.usecase.ObserveRoute
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingEngineTest {
    @Test fun stoppingPersistsMonotonicTimeDespiteWallClockJump() = runTest {
        val repository = MemoryRepository()
        var wall = 1_000L
        val clock = object : TrackingClock {
            override fun wallMillis() = wall
            override fun monotonicMillis() = testScheduler.currentTime
        }
        val source = object : LocationSource {
            override fun points(sessionId: String): Flow<Outcome<LocationPoint>> = flow { awaitCancellation() }
        }
        val engine = TrackingEngine(repository, source, clock) { "session" }
        val job = launch { engine.run() }
        runCurrent()
        wall = 9_999_999L
        advanceTimeBy(5000)
        job.cancelAndJoin()
        assertEquals(5000L, repository.session.value?.elapsedMillis)
        assertFalse(engine.status.value.running)
        assertFalse(engine.status.value.busy)
        assertNotNull(repository.session.value?.endedAt)
    }

    @Test fun diskFullStopsCaptureAndSurfacesTypedFailure() = runTest {
        val repository = MemoryRepository(TrackingFailure.StorageFull)
        val source = object : LocationSource {
            override fun points(sessionId: String) = flowOf(Outcome.Success(point(sessionId, 1)))
        }
        val engine = TrackingEngine(repository, source, object : TrackingClock {
            override fun wallMillis() = 1000L
            override fun monotonicMillis() = 0L
        }) { "session" }
        engine.run()
        assertEquals(TrackingFailure.StorageFull, engine.status.value.failure)
        assertFalse(engine.status.value.running)
        assertTrue(repository.points.value.isEmpty())
    }

    @Test fun incrementalRouteDoesNotDoubleCountRepeatedEmissions() = runTest {
        val repository = MemoryRepository()
        val a = point("s", 1)
        val b = point("s", 2).copy(longitude = 0.001)
        val emissions = mutableListOf<Double>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveRoute(repository)("s").collect { if (it is Outcome.Success) emissions.add(it.value.distanceMeters) }
        }
        repository.points.value = listOf(a, b)
        assertEquals(111.195, emissions.last(), 0.01)
        repository.points.value = listOf(a, b, point("s", 3).copy(longitude = 0.002))
        assertEquals(222.390, emissions.last(), 0.01)
        repository.points.value = listOf(a)
        assertEquals(0.0, emissions.last())
        job.cancelAndJoin()
    }
}

private fun point(session: String, time: Long) = LocationPoint(sessionId = session, latitude = 0.0,
    longitude = 0.0, altitude = null, speed = null, timestamp = time)

private class MemoryRepository(private val appendFailure: TrackingFailure? = null) : ITrackingRepository {
    val session = MutableStateFlow<TrackingSession?>(null)
    val points = MutableStateFlow<List<LocationPoint>>(emptyList())
    override suspend fun createSession(session: TrackingSession): Outcome<Unit> {
        this.session.value = session
        return Outcome.Success(Unit)
    }
    override suspend fun append(point: LocationPoint, elapsedMillis: Long): Outcome<Unit> {
        appendFailure?.let { return Outcome.Failure(it) }
        points.value = points.value + point
        return Outcome.Success(Unit)
    }
    override suspend fun finishSession(id: String, endedAt: Long, elapsedMillis: Long): Outcome<Unit> {
        session.value = session.value?.copy(endedAt = endedAt, elapsedMillis = elapsedMillis)
        return Outcome.Success(Unit)
    }
    override fun observeLatestSession(): Flow<Outcome<TrackingSession?>> = session.map { Outcome.Success(it) }
    override fun observeAllSessions(): Flow<Outcome<List<TrackingSession>>> = flowOf(Outcome.Success(emptyList()))
    override fun observeSession(sessionId: String): Flow<Outcome<TrackingSession?>> = flowOf(Outcome.Success(null))
    override fun observePoints(sessionId: String): Flow<Outcome<List<LocationPoint>>> = points.map { Outcome.Success(it) }
    override suspend fun getPoints(sessionId: String): Outcome<List<LocationPoint>> = Outcome.Success(points.value)
}
