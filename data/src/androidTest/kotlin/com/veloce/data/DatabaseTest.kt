package com.veloce.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veloce.data.local.*
import com.veloce.data.repository.TrackingRepositoryImpl
import com.veloce.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @Test fun walPersistenceOrderingAndIdempotencySurviveReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("veloce.db")
        var database = VeloceDatabase.create(context)
        try {
            var repository = TrackingRepositoryImpl(database.trackingDao())
            assertTrue(repository.createSession(TrackingSession("s", 1)) is Outcome.Success)
            val point = LocationPoint(sessionId = "s", latitude = 0.0, longitude = 0.0,
                altitude = null, speed = null, timestamp = 3)
            assertTrue(repository.append(point, 2000) is Outcome.Success)
            assertTrue(repository.append(point, 2000) is Outcome.Success)
            assertTrue(repository.append(point.copy(timestamp = 2), 1000) is Outcome.Success)
            database.openHelper.writableDatabase.query("PRAGMA journal_mode").use {
                assertTrue(it.moveToFirst()); assertEquals("wal", it.getString(0))
            }
            database.close()
            database = VeloceDatabase.create(context)
            repository = TrackingRepositoryImpl(database.trackingDao())
            val points = repository.observePoints("s").first() as Outcome.Success
            assertEquals(listOf(2L, 3L), points.value.map { it.timestamp })
            val session = repository.observeLatestSession().first() as Outcome.Success
            assertEquals(2000L, session.value!!.elapsedMillis)
            assertTrue(repository.finishSession("s", 5000, 4000) is Outcome.Success)
            assertTrue(repository.append(point.copy(timestamp = 4), 5000) is Outcome.Failure)
        } finally { database.close(); context.deleteDatabase("veloce.db") }
    }
}
