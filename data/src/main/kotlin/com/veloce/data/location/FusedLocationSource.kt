package com.veloce.data.location
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.*
import com.veloce.domain.model.*
import com.veloce.domain.repository.LocationSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

private class CaptureOverflowException : Exception()
class FusedLocationSource(context: Context) : LocationSource {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val locationManager = context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission")
    override fun points(sessionId: String): Flow<Outcome<LocationPoint>> = callbackFlow<Outcome<LocationPoint>> {
        if (!locationManager.enabled()) {
            trySend(Outcome.Failure(TrackingFailure.LocationUnavailable)); close()
            return@callbackFlow
        }
        var lastElapsedNanos = -1L
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.sortedBy { it.elapsedRealtimeNanos }.forEach { location ->
                    val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
                    if (age !in 0..15_000_000_000L || location.elapsedRealtimeNanos <= lastElapsedNanos) return@forEach
                    if (!location.hasAccuracy() || location.accuracy > 30f) return@forEach
                    lastElapsedNanos = location.elapsedRealtimeNanos
                    val point = try {
                        LocationPoint(sessionId = sessionId, latitude = location.latitude, longitude = location.longitude,
                            altitude = location.altitude.takeIf { location.hasAltitude() },
                            speed = location.speed.toDouble().takeIf { location.hasSpeed() && it >= 0 },
                            timestamp = location.time)
                    } catch (_: IllegalArgumentException) { return@forEach }
                    if (trySend(Outcome.Success(point)).isFailure) close(CaptureOverflowException())
                }
            }
            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable && !locationManager.enabled()) {
                    trySend(Outcome.Failure(TrackingFailure.LocationUnavailable)); close()
                }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L).setMinUpdateDistanceMeters(3f)
            .setMaxUpdateDelayMillis(0).setWaitForAccurateLocation(true).build()
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper()).addOnFailureListener { close(it) }
        } catch (error: SecurityException) { close(error) }
        awaitClose { client.removeLocationUpdates(callback) }
    }.buffer(256).catch { error ->
        if (error is kotlinx.coroutines.CancellationException) throw error
        emit(Outcome.Failure(when (error) {
            is SecurityException -> TrackingFailure.PermissionDenied
            is CaptureOverflowException -> TrackingFailure.CaptureOverflow
            else -> TrackingFailure.LocationUnavailable
        }))
    }
}
private fun LocationManager.enabled(): Boolean =
    isProviderEnabled(LocationManager.GPS_PROVIDER) || isProviderEnabled(LocationManager.NETWORK_PROVIDER)
