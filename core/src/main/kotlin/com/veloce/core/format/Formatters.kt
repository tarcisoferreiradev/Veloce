package com.veloce.core.format

import java.util.Locale
import kotlin.math.roundToLong

fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}

fun formatPace(minutesPerKm: Double?): String {
    if (minutesPerKm == null || !minutesPerKm.isFinite() || minutesPerKm <= 0 || minutesPerKm > 40) return "—"
    val seconds = (minutesPerKm * 60).roundToLong()
    return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
}
