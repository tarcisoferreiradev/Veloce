package com.veloce.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.veloce.domain.model.Gender
import com.veloce.domain.model.MeasurementUnit
import com.veloce.domain.model.RegistrationPayload
import com.veloce.domain.usecase.GetSavedTokenUseCase
import com.veloce.domain.usecase.GoogleAuthUseCase
import com.veloce.domain.usecase.LoginUseCase
import com.veloce.domain.usecase.LogoutUseCase
import com.veloce.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthScreenState {
    data object CheckingToken : AuthScreenState()
    data object Welcome : AuthScreenState()
    data object Login : AuthScreenState()
    data class RegisterStep1(val name: String = "", val email: String = "", val password: String = "", val confirm: String = "") : AuthScreenState()
    data class RegisterStep2(
        val name: String, val email: String, val password: String,
        val birthDate: String = "", val gender: Gender = Gender.MALE, val weight: Double = 0.0, val height: Int = 0, val unit: MeasurementUnit = MeasurementUnit.METRIC
    ) : AuthScreenState()
    data class GoogleRegisterStep2(
        val idToken: String, val name: String, val email: String,
        val birthDate: String = "", val gender: Gender = Gender.MALE, val weight: Double = 0.0, val height: Int = 0, val unit: MeasurementUnit = MeasurementUnit.METRIC
    ) : AuthScreenState()
    data object Authenticated : AuthScreenState()
}

data class AuthUiState(
    val screen: AuthScreenState = AuthScreenState.CheckingToken,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val getSavedTokenUseCase: GetSavedTokenUseCase,
    private val googleAuthUseCase: GoogleAuthUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        checkToken()
    }

    private fun checkToken() {
        viewModelScope.launch {
            val token = getSavedTokenUseCase()
            if (token != null) {
                _state.update { it.copy(screen = AuthScreenState.Authenticated) }
            } else {
                _state.update { it.copy(screen = AuthScreenState.Welcome) }
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(error = "Preencha e-mail e senha para continuar.") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            val result = loginUseCase(email, pass)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, screen = AuthScreenState.Authenticated) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "E-mail ou senha incorretos."
                _state.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    fun goToRegister() {
        _state.update { it.copy(screen = AuthScreenState.RegisterStep1(), error = null) }
    }

    fun goToLogin() {
        _state.update { it.copy(screen = AuthScreenState.Login, error = null) }
    }

    fun goToWelcome() {
        _state.update { it.copy(screen = AuthScreenState.Welcome, error = null) }
    }

    fun nextRegisterStep(name: String, email: String, pass: String, confirm: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(error = "Preencha todos os campos obrigatórios.") }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(error = "Formato de e-mail inválido.") }
            return
        }
        if (pass != confirm) {
            _state.update { it.copy(error = "As senhas não coincidem.") }
            return
        }
        if (pass.length < 6) {
            _state.update { it.copy(error = "A senha deve ter pelo menos 6 caracteres.") }
            return
        }
        _state.update { it.copy(error = null, screen = AuthScreenState.RegisterStep2(name, email, pass)) }
    }

    fun completeRegistration(birthDate: String, gender: Gender, weight: Double, height: Int, unit: MeasurementUnit) {
        val currentScreen = _state.value.screen as? AuthScreenState.RegisterStep2 ?: return
        
        if (birthDate.isBlank() || weight <= 0 || height <= 0) {
            _state.update { it.copy(error = "Preencha todas as métricas biométricas com valores válidos.") }
            return
        }
        
        val payload = RegistrationPayload(
            name = currentScreen.name,
            email = currentScreen.email,
            password = currentScreen.password,
            birthDate = birthDate,
            gender = gender,
            weight = weight,
            height = height,
            unit = unit
        )

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            val result = registerUseCase(payload)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, screen = AuthScreenState.Authenticated) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Erro ao realizar o cadastro."
                _state.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    fun completeGoogleRegistration(birthDate: String, gender: Gender, weight: Double, height: Int, unit: MeasurementUnit) {
        val currentScreen = _state.value.screen as? AuthScreenState.GoogleRegisterStep2 ?: return

        if (birthDate.isBlank() || weight <= 0 || height <= 0) {
            _state.update { it.copy(error = "Preencha todas as métricas biométricas corretamente.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            val result = googleAuthUseCase(currentScreen.idToken)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, screen = AuthScreenState.Authenticated) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Falha ao autenticar com o Google."
                _state.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _state.update { it.copy(screen = AuthScreenState.Welcome) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun launchGoogleSignIn(context: Context) {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("226557078570-qmudv8u5ma0q651j94tb9vi84agt4ali.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val name = googleIdTokenCredential.displayName ?: "Usuário Google"
                    val email = googleIdTokenCredential.id

                    // Transition to Google Biometrics Onboarding Step
                    _state.update {
                        it.copy(
                            screen = AuthScreenState.GoogleRegisterStep2(
                                idToken = idToken,
                                name = name,
                                email = email
                            ),
                            error = null
                        )
                    }
                } else {
                    _state.update { it.copy(error = "Tipo de credencial não suportado.") }
                }
            } catch (e: GetCredentialException) {
                _state.update { it.copy(error = "Login com Google cancelado ou indisponível.") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro no login com Google: ${e.localizedMessage}") }
            }
        }
    }

    class Factory(
        private val loginUseCase: LoginUseCase,
        private val registerUseCase: RegisterUseCase,
        private val getSavedTokenUseCase: GetSavedTokenUseCase,
        private val googleAuthUseCase: GoogleAuthUseCase,
        private val logoutUseCase: LogoutUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(loginUseCase, registerUseCase, getSavedTokenUseCase, googleAuthUseCase, logoutUseCase) as T
    }
}
