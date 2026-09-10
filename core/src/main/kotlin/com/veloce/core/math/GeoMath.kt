package com.veloce.core.math

import kotlin.math.*

private const val EARTH_RADIUS_METERS = 6_371_008.8

/** Degrees in, meters out. Pure and stable at antipodes and the date line. */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    require(listOf(lat1, lon1, lat2, lon2).all(Double::isFinite))
    require(lat1 in -90.0..90.0 && lat2 in -90.0..90.0)
    require(lon1 in -180.0..180.0 && lon2 in -180.0..180.0)
    val deltaLat = Math.toRadians(lat2 - lat1)
    val deltaLon = Math.toRadians(lon2 - lon1)
    val a = sin(deltaLat / 2).pow(2) + cos(Math.toRadians(lat1)) *
        cos(Math.toRadians(lat2)) * sin(deltaLon / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

/** null means unavailable; values slower than 40 min/km are discarded, not clamped. */
fun paceMinutesPerKm(distanceMeters: Double, durationSeconds: Double): Double? {
    if (!distanceMeters.isFinite() || !durationSeconds.isFinite()) return null
    if (distanceMeters <= 0 || durationSeconds <= 0) return null
    return (durationSeconds / 60 / (distanceMeters / 1000))
        .takeIf { it.isFinite() && it > 0 && it <= 40 }
}
