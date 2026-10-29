package com.veloce.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veloce.VeloceApplication
import com.veloce.presentation.tracking.*
import com.veloce.presentation.ui.*
import com.veloce.presentation.auth.AuthScreen
import com.veloce.presentation.auth.AuthScreenState
import com.veloce.presentation.auth.AuthViewModel
import com.veloce.presentation.history.HistoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val graph = (application as VeloceApplication).graph
        setContent {
            VeloceTheme {
                val authModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(
                        graph.loginUseCase,
                        graph.registerUseCase,
                        graph.getSavedTokenUseCase,
                        graph.googleAuthUseCase,
                        graph.logoutUseCase
                    )
                )
                val authState by authModel.state.collectAsStateWithLifecycle()

                if (authState.screen !is AuthScreenState.Authenticated) {
                    AuthScreen(
                        state = authState,
                        onLogin = authModel::login,
                        onGoToRegister = authModel::goToRegister,
                        onGoToLogin = authModel::goToLogin,
                        onGoToWelcome = authModel::goToWelcome,
                        onNextStep = authModel::nextRegisterStep,
                        onCompleteRegistration = authModel::completeRegistration,
                        onCompleteGoogleRegistration = authModel::completeGoogleRegistration,
                        onGoogleAuth = { authModel.launchGoogleSignIn(this@MainActivity) },
                        onClearError = authModel::clearError,
                        onAuthenticated = { /* Already handled by state change */ }
                    )
                } else {
                    var selectedTab by rememberSaveable { mutableStateOf(0) }
                    
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Atividade") },
                                    label = { Text("Atividade") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Histórico") },
                                    label = { Text("Histórico") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                                    label = { Text("Perfil") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = androidx.compose.ui.Modifier.padding(top = 0.dp, bottom = innerPadding.calculateBottomPadding())) {
                            when (selectedTab) {
                                0 -> {
                                    val model: TrackingViewModel = viewModel(factory = TrackingViewModel.Factory(graph.repository, graph.controller))
                                    val state by model.state.collectAsStateWithLifecycle()
                                    var permissionMessage by rememberSaveable { mutableStateOf<String?>(null) }
                                    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                                        if (hasPreciseLocation()) {
                                            permissionMessage = null
                                        } else permissionMessage = "Permita localização precisa para registrar a rota. Se a permissão foi bloqueada, abra as configurações do app."
                                    }
                                    
                                    LaunchedEffect(Unit) {
                                        if (!hasPreciseLocation()) {
                                            permissions.launch(buildList {
                                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                            }.toTypedArray())
                                        }
                                    }

                                    TrackingScreen(state, permissionMessage, onStart = {
                                        if (hasPreciseLocation() && (Build.VERSION.SDK_INT < 33 ||
                                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
                                            permissionMessage = null
                                            model.start()
                                        } else permissions.launch(buildList {
                                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                        }.toTypedArray())
                                    }, onStop = model::stop, onSettings = {
                                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    }, onAppSettings = {
                                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            android.net.Uri.parse("package:$packageName")))
                                    })
                                }
                                1 -> {
                                    val historyModel: HistoryViewModel = viewModel(
                                        factory = HistoryViewModel.Factory(graph.getHistorySessionsUseCase, graph.exportSessionUseCase, graph.repository)
                                    )
                                    val historyState by historyModel.state.collectAsStateWithLifecycle()
                                    HistoryScreen(
                                        state = historyState,
                                        onBack = { selectedTab = 0 },
                                        onExport = historyModel::exportSession,
                                        onClearExport = historyModel::clearExport,
                                        onLoadPoints = historyModel::loadPoints
                                    )
                                }
                                2 -> {
                                    ProfileScreen(onLogout = authModel::logout)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun hasPreciseLocation() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}
