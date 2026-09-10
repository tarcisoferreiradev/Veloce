package com.veloce.data.repository

import android.content.Context
import com.veloce.domain.model.AuthToken
import com.veloce.domain.model.RegistrationPayload
import com.veloce.domain.repository.IAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AuthRepositoryImpl(context: Context) : IAuthRepository {

    private val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val baseUrl = "https://veloce-back.onrender.com"

    override suspend fun login(email: String, password: String): Result<AuthToken> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        
        val (code, body) = performRequest("/auth/login", payload)
        
        when (code) {
            200 -> Result.success(AuthToken(JSONObject(body).getString("token")))
            401 -> Result.failure(Exception("E-mail ou senha incorretos."))
            else -> Result.failure(parseError(code, body))
        }
    }

    override suspend fun register(payload: RegistrationPayload): Result<AuthToken> = withContext(Dispatchers.IO) {
        val jsonPayload = JSONObject().apply {
            put("name", payload.name)
            put("email", payload.email)
            put("password", payload.password)
            put("weight", payload.weight)
            put("height", payload.height)
            put("birthDate", payload.birthDate)
            put("biologicalSex", payload.gender.apiValue)
            put("unitSystem", payload.unit.apiValue)
        }
        
        val (code, body) = performRequest("/auth/register", jsonPayload)
        
        when (code) {
            200, 201 -> Result.success(AuthToken(JSONObject(body).getString("token")))
            400 -> Result.failure(Exception("Dados inválidos. Verifique os campos preenchidos."))
            409 -> Result.failure(Exception("Este e-mail já está em uso."))
            else -> Result.failure(parseError(code, body))
        }
    }

    override suspend fun googleAuth(idToken: String): Result<AuthToken> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("idToken", idToken)
        }
        
        val (code, body) = performRequest("/auth/google", payload)
        
        when (code) {
            200, 201 -> Result.success(AuthToken(JSONObject(body).getString("token")))
            400, 401 -> Result.failure(Exception("Falha na autenticação do Google. Token inválido."))
            else -> Result.failure(parseError(code, body))
        }
    }

    override suspend fun saveToken(token: AuthToken) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString("jwt_token", token.token).apply()
    }

    override suspend fun getToken(): AuthToken? = withContext(Dispatchers.IO) {
        val tokenStr = sharedPreferences.getString("jwt_token", null) ?: return@withContext null
        AuthToken(tokenStr)
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove("jwt_token").apply()
    }

    /**
     * Cliente HTTP centralizado. Injeta o cabeçalho Authorization se o token existir.
     * Retorna o HTTP Status Code e a string de resposta (body ou errorStream).
     */
    private fun performRequest(endpoint: String, payload: JSONObject): Pair<Int, String> {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            
            // Injeção do token Bearer
            val currentToken = sharedPreferences.getString("jwt_token", null)
            if (!currentToken.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $currentToken")
            }
            
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
            
            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            
            Pair(responseCode, responseBody)
        } catch (e: Exception) {
            Pair(0, e.message ?: "Erro de conexão")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseError(code: Int, body: String): Exception {
        if (code == 0) return Exception("Não foi possível conectar ao servidor. Verifique sua rede.")
        
        return try {
            val message = JSONObject(body).getString("message")
            Exception(message)
        } catch (e: Exception) {
            Exception("Erro inesperado no servidor (Código: $code).")
        }
    }
}
