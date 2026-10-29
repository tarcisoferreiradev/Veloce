package com.veloce.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veloce.core.format.*
import com.veloce.domain.model.TrackingFailure
import com.veloce.presentation.map.RouteMap
import com.veloce.presentation.tracking.TrackingUiState
import java.util.Locale

@Composable
fun TrackingScreen(
    state: TrackingUiState, permissionMessage: String?,
    onStart: () -> Unit, onStop: () -> Unit, onSettings: () -> Unit, onAppSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // Fullscreen map in background
        RouteMap(state.route.points, Modifier.fillMaxSize())

        if (state.running) {
            // ==================== RUNNING STATE (ATIVIDADE EM ANDAMENTO) ====================
            // Top Header: Distance and Time with a subtle top vignette gradient effect for legibility
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "DISTÂNCIA • km",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                    Text(
                        String.format(Locale("pt", "BR"), "%.2f", state.route.distanceMeters / 1000),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum")
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "TEMPO",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                    Text(
                        formatDuration(state.elapsedSeconds),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum")
                    )
                }
            }

            // Bottom Action Bar: Only Finalizar Atividade button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Button(
                    onClick = onStop,
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        "Finalizar atividade",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            // ==================== IDLE STATE (PRONTO PARA INICIAR) ====================
            // Only the "Iniciar nova atividade" button positioned slightly lower down
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val error = permissionMessage ?: state.failure?.message()
                if (error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            Row {
                                TextButton(onClick = onSettings) { Text("Ativar GPS") }
                                TextButton(onClick = onAppSettings) { Text("Permissões") }
                            }
                        }
                    }
                }

                Button(
                    onClick = onStart,
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        "Iniciar nova atividade",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

private fun TrackingFailure.message(): String = when (this) {
    TrackingFailure.StorageFull -> "Armazenamento cheio. Libere espaço antes de iniciar outra atividade."
    TrackingFailure.StorageUnavailable -> "Não foi possível acessar o banco local. A captura foi interrompida."
    TrackingFailure.StorageCorrupt -> "O banco local apresentou corrupção. Os dados não foram apagados."
    TrackingFailure.StorageBusy -> "Banco ocupado. Tente novamente."
    TrackingFailure.InvalidData -> "Não foi possível gravar os dados da sessão."
    TrackingFailure.PermissionDenied -> "Localização precisa não autorizada. Verifique as permissões."
    TrackingFailure.LocationUnavailable -> "GPS indisponível. Ative a localização e verifique o Google Play Services."
    TrackingFailure.CaptureOverflow -> "A gravação não acompanhou o GPS. Captura interrompida para evitar perda silenciosa."
    TrackingFailure.Unexpected -> "A atividade foi interrompida por um erro inesperado."
}
