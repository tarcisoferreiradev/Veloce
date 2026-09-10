package com.veloce.data.repository
import android.database.sqlite.*
import com.veloce.data.local.*
import com.veloce.domain.model.*
import com.veloce.domain.repository.ITrackingRepository
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TrackingRepositoryImpl(private val dao: TrackingDao, private val io: CoroutineDispatcher = Dispatchers.IO) : ITrackingRepository {
    override suspend fun createSession(session: TrackingSession) = write { dao.createSession(session.toEntity()) }
    override suspend fun append(point: LocationPoint, elapsedMillis: Long) = write { dao.append(point.toEntity(), elapsedMillis) }
    override suspend fun finishSession(id: String, endedAt: Long, elapsedMillis: Long) = write {
        dao.finish(id, endedAt, elapsedMillis)
        Unit
    }
    override fun observeLatestSession(): Flow<Outcome<TrackingSession?>> =
        dao.observeLatestSession().map { it?.toDomain() }.asOutcome()
    override fun observeAllSessions(): Flow<Outcome<List<TrackingSession>>> =
        dao.observeAllSessions().map { rows -> rows.map { it.toDomain() } }.asOutcome()
    override fun observeSession(sessionId: String): Flow<Outcome<TrackingSession?>> =
        dao.observeSession(sessionId).map { it?.toDomain() }.asOutcome()
    override fun observePoints(sessionId: String): Flow<Outcome<List<LocationPoint>>> =
        dao.observePoints(sessionId).map { rows -> rows.map { it.toDomain() } }.asOutcome()

    override suspend fun getPoints(sessionId: String): Outcome<List<LocationPoint>> = withContext(io) {
        try {
            Outcome.Success(dao.getPoints(sessionId).map { it.toDomain() })
        } catch (e: Exception) {
            Outcome.Failure(e.toFailure())
        }
    }

    private suspend fun write(block: suspend () -> Unit): Outcome<Unit> = withContext(io) {
        var attempt = 0
        while (true) {
            try {
                block()
                return@withContext Outcome.Success(Unit)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (error is SQLiteDatabaseLockedException && attempt < 2) delay(100L * (1 shl attempt++))
                else return@withContext Outcome.Failure(error.toFailure())
            }
        }
        @Suppress("UNREACHABLE_CODE")
        Outcome.Failure(TrackingFailure.Unexpected)
    }
    private fun <T> Flow<T>.asOutcome(): Flow<Outcome<T>> =
        map<T, Outcome<T>> { Outcome.Success(it) }
            .retryWhen { error, attempt ->
                if (error is SQLiteDatabaseLockedException && attempt < 2) {
                    delay(100L * (1L shl attempt.toInt())); true
                } else false
            }.catch { error ->
                if (error is CancellationException) throw error
                emit(Outcome.Failure(error.toFailure()))
            }.flowOn(io)
}
internal fun Throwable.toFailure(): TrackingFailure = when (this) {
    is SQLiteFullException -> TrackingFailure.StorageFull
    is SQLiteDatabaseCorruptException -> TrackingFailure.StorageCorrupt
    is SQLiteDatabaseLockedException -> TrackingFailure.StorageBusy
    is SQLiteConstraintException, is IllegalArgumentException, is IllegalStateException -> TrackingFailure.InvalidData
    is SQLiteException, is IOException -> TrackingFailure.StorageUnavailable
    is SecurityException -> TrackingFailure.PermissionDenied
    else -> TrackingFailure.Unexpected
}
