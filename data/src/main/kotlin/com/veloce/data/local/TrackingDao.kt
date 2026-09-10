package com.veloce.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createSession(session: SessionEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoint(point: LocationPointEntity): Long
    @Query("UPDATE sessions SET elapsed_millis = MAX(elapsed_millis, :elapsed) WHERE id = :id AND ended_at IS NULL")
    suspend fun checkpoint(id: String, elapsed: Long): Int
    @Transaction
    suspend fun append(point: LocationPointEntity, elapsed: Long) {
        check(checkpoint(point.sessionId, elapsed) == 1) { "Session not active" }
        insertPoint(point)
    }
    @Query("UPDATE sessions SET ended_at = :endedAt, elapsed_millis = MAX(elapsed_millis, :elapsed) WHERE id = :id AND ended_at IS NULL")
    suspend fun finish(id: String, endedAt: Long, elapsed: Long): Int
    @Query("SELECT * FROM location_points WHERE session_id = :sessionId ORDER BY timestamp ASC, id ASC")
    fun observePoints(sessionId: String): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE session_id = :sessionId ORDER BY timestamp ASC, id ASC")
    suspend fun getPoints(sessionId: String): List<LocationPointEntity>
    @Query("SELECT * FROM sessions ORDER BY started_at DESC, id DESC LIMIT 1")
    fun observeLatestSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY started_at DESC, id DESC")
    fun observeAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSession(sessionId: String): Flow<SessionEntity?>
}
