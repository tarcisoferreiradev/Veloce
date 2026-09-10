package com.veloce.domain.tracking

import com.veloce.domain.model.TrackingFailure
import kotlinx.coroutines.flow.StateFlow

/** Port implemented by the Android service adapter. */
interface TrackingController {
    val status: StateFlow<TrackingStatus>
    fun start()
    fun stop()
}

data class TrackingStatus(
    val running: Boolean = false,
    val busy: Boolean = false,
    val sessionId: String? = null,
    val elapsedMillis: Long = 0,
    val failure: TrackingFailure? = null,
)
