package com.example.network

data class SimpleResponse(
    val status: String,
    val message: String? = null,
    val id: Long? = null,
    val role: String? = null,
    val token: String? = null
)
