package com.veloce.presentation.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.veloce.core.format.formatDuration
import com.veloce.core.format.formatPace
import com.veloce.domain.usecase.ExportFormat
import com.veloce.domain.usecase.HistorySession
import com.veloce.presentation.history.HistoryUiState
import java.io.File
import java.text.SimpleDateFormat
import com.veloce.presentation.map.RouteMap
import com.veloce.domain.model.LocationPoint
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onExport: (String, ExportFormat) -> Unit,
    onClearExport: () -> Unit,
    onLoadPoints: (String) -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(state.exportData) {
        if (state.exportData != null && state.exportFormat != null) {
            shareFile(context, state.exportData, state.exportFormat)
            onClearExport()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Atividades") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhuma atividade encontrada.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(state.sessions) { session ->
                    SessionItem(
                        session = session,
                        points = state.sessionPoints[session.session.id],
                        onExport = onExport,
                        onExpand = { onLoadPoints(session.session.id) }
                    )
                }
            }
        }
        
        state.exportError?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun SessionItem(
    session: HistorySession,
    points: List<LocationPoint>?,
    onExport: (String, ExportFormat) -> Unit,
    onExpand: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val startDate = dateFormat.format(Date(session.session.startedAt))
    
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().clickable { 
        expanded = !expanded
        if (expanded) onExpand()
    }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Data: $startDate", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Distância: ${String.format(Locale("pt", "BR"), "%.2f", session.distanceMeters / 1000)} km")
                Text("Tempo: ${formatDuration(session.session.elapsedMillis / 1000)}")
            }
            if (session.pace != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ritmo Médio: ${formatPace(session.pace)} min/km")
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                if (points != null && points.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        RouteMap(points = points, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (points == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { onExport(session.session.id, ExportFormat.GPX) }) {
                        Icon(Icons.Default.Share, contentDescription = "GPX")
                        Spacer(Modifier.width(4.dp))
                        Text("GPX")
                    }
                    Button(onClick = { onExport(session.session.id, ExportFormat.JSON) }) {
                        Icon(Icons.Default.Share, contentDescription = "JSON")
                        Spacer(Modifier.width(4.dp))
                        Text("JSON")
                    }
                }
            }
        }
    }
}

private fun shareFile(context: Context, data: String, format: ExportFormat) {
    val extension = if (format == ExportFormat.GPX) "gpx" else "json"
    val mimeType = if (format == ExportFormat.GPX) "application/gpx+xml" else "application/json"
    val fileName = "veloce_export_${System.currentTimeMillis()}.$extension"
    
    val cacheDir = File(context.cacheDir, "exports")
    cacheDir.mkdirs()
    val file = File(cacheDir, fileName)
    file.writeText(data)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar via"))
}
