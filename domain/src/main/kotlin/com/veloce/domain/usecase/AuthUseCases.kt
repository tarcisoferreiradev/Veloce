package com.veloce.domain.usecase

import com.veloce.domain.model.AuthToken
import com.veloce.domain.model.RegistrationPayload
import com.veloce.domain.repository.IAuthRepository

class LoginUseCase(private val repository: IAuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthToken> {
        val result = repository.login(email, password)
        if (result.isSuccess) {
            repository.saveToken(result.getOrThrow())
        }
        return result
    }
}

class RegisterUseCase(private val repository: IAuthRepository) {
    suspend operator fun invoke(payload: RegistrationPayload): Result<AuthToken> {
        val result = repository.register(payload)
        if (result.isSuccess) {
            repository.saveToken(result.getOrThrow())
        }
        return result
    }
}

class GetSavedTokenUseCase(private val repository: IAuthRepository) {
    suspend operator fun invoke(): AuthToken? = repository.getToken()
}

class GoogleAuthUseCase(private val repository: IAuthRepository) {
    suspend operator fun invoke(idToken: String): Result<AuthToken> {
        val result = repository.googleAuth(idToken)
        if (result.isSuccess) {
            repository.saveToken(result.getOrThrow())
        }
        return result
    }
}

class LogoutUseCase(private val repository: IAuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
