package com.veloce.presentation.tracking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.veloce.domain.model.TrackingFailure
import com.veloce.domain.tracking.*

class AndroidTrackingController(private val context: Context, private val engine: TrackingEngine) : TrackingController {
    override val status = engine.status
    override fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            engine.report(TrackingFailure.PermissionDenied)
            return
        }
        try {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java).setAction(TrackingService.START))
        } catch (_: SecurityException) { engine.report(TrackingFailure.PermissionDenied)
        } catch (_: IllegalStateException) { engine.report(TrackingFailure.LocationUnavailable) }
    }
    override fun stop() {
        context.startService(Intent(context, TrackingService::class.java).setAction(TrackingService.STOP))
    }
}
