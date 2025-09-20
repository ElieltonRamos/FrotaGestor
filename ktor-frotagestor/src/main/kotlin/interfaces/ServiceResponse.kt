package com.redenorte.interfaces

import io.ktor.http.HttpStatusCode

data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class ServiceResponse<T>(
    val status: HttpStatusCode,
    val data: T
)