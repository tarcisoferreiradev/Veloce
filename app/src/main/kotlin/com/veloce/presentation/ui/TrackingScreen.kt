package com.veloce.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("veloce", fontSize = 32.sp, fontWeight = FontWeight.Black)
                AssistChip(onClick = {}, label = { Text(if (state.running) "● GRAVANDO" else "OFFLINE") })
            }
            Text(when {
                state.busy -> "Salvando atividade…"
                state.running -> "Seu movimento, seu ritmo."
                state.interrupted -> "Atividade interrompida. Dados gravados preservados."
                state.session != null -> "Última atividade salva neste dispositivo."
                else -> "Tudo pronto para sair?"
            }, style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Metric("DISTÂNCIA • km", String.format(Locale("pt", "BR"), "%.2f", state.route.distanceMeters / 1000), 48)
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric("RITMO MÉDIO • min/km", formatPace(state.pace), 24)
                        Metric("TEMPO", formatDuration(state.elapsedSeconds), 24)
                    }
                }
            }
            Card(Modifier.fillMaxWidth().height(300.dp)) { RouteMap(state.route.points, Modifier.fillMaxSize()) }
            Text(if (state.route.points.isEmpty()) "Aguardando posição GPS precisa. Procure um local aberto."
                else "${state.route.points.size} pontos salvos • rota disponível sem internet",
                style = MaterialTheme.typography.bodySmall)
            Text("Mapa offline da rota • sem mapa de ruas", style = MaterialTheme.typography.labelSmall)
            val error = permissionMessage ?: state.failure?.message()
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
                Row {
                    TextButton(onClick = onSettings) { Text("Ativar GPS") }
                    TextButton(onClick = onAppSettings) { Text("Permissões") }
                }
            }
            Button(onClick = if (state.running) onStop else onStart, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(if (state.running) "Finalizar atividade" else "Iniciar nova atividade")
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, size: Int) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
            fontSize = size.sp, style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"))
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
