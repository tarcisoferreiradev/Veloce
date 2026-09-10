package com.veloce.data.local
import androidx.room.*
import com.veloce.domain.model.*

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "elapsed_millis") val elapsedMillis: Long,
)
@Entity(tableName = "location_points",
    foreignKeys = [ForeignKey(entity = SessionEntity::class, parentColumns = ["id"],
        childColumns = ["session_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["session_id", "timestamp"], unique = true)])
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val latitude: Double, val longitude: Double,
    val altitude: Double?, val speed: Double?, val timestamp: Long,
)
internal fun LocationPointEntity.toDomain() = LocationPoint(id, sessionId, latitude, longitude, altitude, speed, timestamp)
internal fun LocationPoint.toEntity() = LocationPointEntity(id, sessionId, latitude, longitude, altitude, speed, timestamp)
internal fun SessionEntity.toDomain() = TrackingSession(id, startedAt, endedAt, elapsedMillis)
internal fun TrackingSession.toEntity() = SessionEntity(id, startedAt, endedAt, elapsedMillis)
