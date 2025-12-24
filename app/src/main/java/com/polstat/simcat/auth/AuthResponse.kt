package com.polstat.simcat.auth

data class AuthResponse(
    val email: String?,
    val accessToken: String?,
    val message: String,
    val role: String?
)