package com.veloce

import android.app.Application
import android.os.SystemClock
import com.veloce.data.local.VeloceDatabase
import com.veloce.data.location.FusedLocationSource
import com.veloce.data.repository.TrackingRepositoryImpl
import com.veloce.domain.repository.ITrackingRepository
import com.veloce.domain.tracking.*
import com.veloce.domain.usecase.ExportSessionUseCase
import com.veloce.domain.usecase.GetHistorySessionsUseCase
import com.veloce.domain.usecase.LoginUseCase
import com.veloce.domain.usecase.RegisterUseCase
import com.veloce.domain.usecase.GetSavedTokenUseCase
import com.veloce.domain.usecase.GoogleAuthUseCase
import com.veloce.domain.usecase.LogoutUseCase
import com.veloce.domain.repository.IAuthRepository
import com.veloce.data.repository.AuthRepositoryImpl
import com.veloce.presentation.tracking.AndroidTrackingController
import java.util.UUID
import org.maplibre.android.MapLibre

class VeloceApplication : Application() {
    val graph by lazy { AppGraph(this) }
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}

/** Composition root: the only place that wires concrete data and presentation adapters. */
class AppGraph(application: Application) {
    private val database = VeloceDatabase.create(application)
    val repository: ITrackingRepository = TrackingRepositoryImpl(database.trackingDao())
    val engine = TrackingEngine(repository, FusedLocationSource(application), object : TrackingClock {
        override fun wallMillis() = System.currentTimeMillis()
        override fun monotonicMillis() = SystemClock.elapsedRealtime()
    }) { UUID.randomUUID().toString() }
    val controller: TrackingController = AndroidTrackingController(application, engine)
    val getHistorySessionsUseCase = GetHistorySessionsUseCase(repository)
    val exportSessionUseCase = ExportSessionUseCase(repository)

    val authRepository: IAuthRepository = AuthRepositoryImpl(application)
    val loginUseCase = LoginUseCase(authRepository)
    val registerUseCase = RegisterUseCase(authRepository)
    val getSavedTokenUseCase = GetSavedTokenUseCase(authRepository)
    val googleAuthUseCase = GoogleAuthUseCase(authRepository)
    val logoutUseCase = LogoutUseCase(authRepository)
}
