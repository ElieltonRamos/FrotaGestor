package com.frotagestor.interfaces

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

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

@Serializable
data class Message (
    val message: String
)