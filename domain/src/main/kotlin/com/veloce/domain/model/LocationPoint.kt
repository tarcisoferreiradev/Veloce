package com.veloce.domain.model

/** WGS84 degrees; altitude meters; speed m/s; timestamp UTC epoch milliseconds. */
data class LocationPoint(
    val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speed: Double?,
    val timestamp: Long,
) {
    init {
        require(sessionId.isNotBlank() && id >= 0 && timestamp >= 0)
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(altitude == null || altitude.isFinite())
        require(speed == null || (speed.isFinite() && speed >= 0))
    }
}

data class TrackingSession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val elapsedMillis: Long = 0,
)
