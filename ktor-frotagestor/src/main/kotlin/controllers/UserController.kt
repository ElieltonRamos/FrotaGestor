package com.redenorte.controllers

import com.redenorte.models.LoginRequest
import com.redenorte.services.UserService
import com.redenorte.utils.mapHttpStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

class UserController(private val userService: UserService) {
    private val internalMsgError = "Internal server error"

    suspend fun login(call: ApplicationCall) {
        try {
            val req = call.receive<LoginRequest>()
            val result = userService.login(req.username, req.password)
            call.respond(mapHttpStatus(result.status), result.data)
        } catch (e: Exception) {
            println("Error in login controller: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun create(call: ApplicationCall) {
        try {
            val req = call.receive<CreateUserRequest>()
            val result = userService.create(req)
            call.respond(mapHttpStatus(result.status), result.data)
        } catch (e: Exception) {
            println("Error in create controller: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}