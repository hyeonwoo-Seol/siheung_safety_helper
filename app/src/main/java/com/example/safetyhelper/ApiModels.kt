package com.example.safetyhelper

data class ApiRequest(
    val location: String,
    val name:     String,
    val issue:    String
)

data class ApiResponse(
    val result: String
)