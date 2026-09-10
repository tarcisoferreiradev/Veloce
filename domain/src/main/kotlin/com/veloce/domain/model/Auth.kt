package com.veloce.domain.model

data class AuthToken(val token: String)

enum class Gender(val apiValue: String) {
    MALE("male"), FEMALE("female"), OTHER("other");
}

enum class MeasurementUnit(val apiValue: String) {
    METRIC("metric"), IMPERIAL("imperial");
}

data class RegistrationPayload(
    val name: String,
    val email: String,
    val password: String,
    val birthDate: String, // YYYY-MM-DD
    val gender: Gender,
    val weight: Double,
    val height: Int,
    val unit: MeasurementUnit
)

sealed interface AuthFailure {
    data object Unauthorized : AuthFailure
    data object NetworkError : AuthFailure
    data class Unknown(val message: String) : AuthFailure
}
