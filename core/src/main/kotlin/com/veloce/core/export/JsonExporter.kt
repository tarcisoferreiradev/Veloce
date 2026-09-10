package com.veloce.core.export

object JsonExporter {
    fun export(points: List<ExportPoint>): String {
        val builder = java.lang.StringBuilder()
        builder.append("[\n")
        points.forEachIndexed { index, p ->
            builder.append("  {\n")
            builder.append("    \"lat\": ${p.latitude},\n")
            builder.append("    \"lon\": ${p.longitude},\n")
            if (p.altitude != null) builder.append("    \"alt\": ${p.altitude},\n")
            builder.append("    \"time\": ${p.timestamp}\n")
            builder.append("  }")
            if (index < points.size - 1) builder.append(",")
            builder.append("\n")
        }
        builder.append("]")
        return builder.toString()
    }
}
