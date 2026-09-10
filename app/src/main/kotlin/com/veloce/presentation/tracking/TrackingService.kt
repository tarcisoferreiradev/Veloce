package com.veloce.presentation.tracking

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.veloce.R
import com.veloce.VeloceApplication
import com.veloce.domain.model.TrackingFailure
import com.veloce.presentation.MainActivity
import kotlinx.coroutines.*

class TrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine get() = (application as VeloceApplication).graph.engine
    private var trackingJob: Job? = null
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Atividade em andamento", NotificationManager.IMPORTANCE_LOW),
        )
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP -> stopTracking()
            START -> startTracking()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        if (trackingJob?.isActive == true || stopping) return
        try {
            ServiceCompat.startForeground(this, NOTIFICATION, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } catch (_: SecurityException) {
            engine.report(TrackingFailure.PermissionDenied); stopSelf(); return
        } catch (_: RuntimeException) {
            engine.report(TrackingFailure.LocationUnavailable); stopSelf(); return
        }
        trackingJob = scope.launch {
            try { engine.run() }
            finally {
                stopping = true
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopTracking() {
        if (stopping) return
        stopping = true
        scope.launch {
            trackingJob?.cancelAndJoin()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, TrackingService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_veloce).setContentTitle("Veloce • atividade em andamento")
            .setContentText("Rastreamento GPS ativo. Toque para ver sua rota.")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_veloce, "Finalizar", stop).build()
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object {
        const val START = "com.veloce.START"
        const val STOP = "com.veloce.STOP"
        private const val CHANNEL = "tracking"
        private const val NOTIFICATION = 101
    }
}
