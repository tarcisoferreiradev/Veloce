package com.veloce.domain.usecase

import com.veloce.core.export.ExportPoint
import com.veloce.core.export.GpxExporter
import com.veloce.core.export.JsonExporter
import com.veloce.domain.model.Outcome
import com.veloce.domain.repository.ITrackingRepository

enum class ExportFormat {
    GPX, JSON
}

class ExportSessionUseCase(
    private val repository: ITrackingRepository
) {
    suspend operator fun invoke(sessionId: String, format: ExportFormat): Outcome<String> {
        val pointsResult = repository.getPoints(sessionId)
        if (pointsResult !is Outcome.Success) {
            return Outcome.Failure((pointsResult as Outcome.Failure).error)
        }
        val exportPoints = pointsResult.value.map { 
            ExportPoint(it.latitude, it.longitude, it.altitude, it.timestamp) 
        }
        val data = when (format) {
            ExportFormat.GPX -> GpxExporter.export(exportPoints)
            ExportFormat.JSON -> JsonExporter.export(exportPoints)
        }
        return Outcome.Success(data)
    }
}
