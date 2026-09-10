package com.veloce.domain.repository

import com.veloce.domain.model.AuthToken
import com.veloce.domain.model.RegistrationPayload

interface IAuthRepository {
    suspend fun login(email: String, password: String): Result<AuthToken>
    suspend fun register(payload: RegistrationPayload): Result<AuthToken>
    suspend fun googleAuth(idToken: String): Result<AuthToken>
    suspend fun saveToken(token: AuthToken)
    suspend fun getToken(): AuthToken?
    suspend fun logout()
}
