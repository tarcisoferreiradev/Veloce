package com.veloce.core.export

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ExportPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val timestamp: Long
)

object GpxExporter {
    fun export(points: List<ExportPoint>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val builder = java.lang.StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<gpx version=\"1.1\" creator=\"Veloce\">\n")
        builder.append("  <trk>\n")
        builder.append("    <trkseg>\n")
        for (p in points) {
            builder.append("      <trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\">\n")
            if (p.altitude != null) {
                builder.append("        <ele>${p.altitude}</ele>\n")
            }
            builder.append("        <time>${dateFormat.format(Date(p.timestamp))}</time>\n")
            builder.append("      </trkpt>\n")
        }
        builder.append("    </trkseg>\n")
        builder.append("  </trk>\n")
        builder.append("</gpx>")
        return builder.toString()
    }
}
