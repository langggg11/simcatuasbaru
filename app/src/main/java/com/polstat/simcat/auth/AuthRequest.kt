package com.polstat.simcat.auth

data class AuthRequest(
    val email: String,
    val password: String
)