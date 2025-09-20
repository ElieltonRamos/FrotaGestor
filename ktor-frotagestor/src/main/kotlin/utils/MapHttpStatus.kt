package com.redenorte.utils

import io.ktor.http.*

fun mapHttpStatus(status: String): HttpStatusCode {
    return when (status) {
        "SUCCESS" -> HttpStatusCode.OK
        "CREATED" -> HttpStatusCode.Created
        "NOT_FOUND" -> HttpStatusCode.NotFound
        "BAD_REQUEST" -> HttpStatusCode.BadRequest
        "UNAUTHORIZED" -> HttpStatusCode.Unauthorized
        "SERVER_ERROR" -> HttpStatusCode.InternalServerError
        else -> HttpStatusCode.InternalServerError
    }
}