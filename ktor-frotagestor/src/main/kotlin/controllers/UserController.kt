package com.frotagestor.controllers

import com.frotagestor.interfaces.LoginRequest
import com.frotagestor.interfaces.User
import com.frotagestor.services.UserService
import com.frotagestor.validations.validateUser
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import com.frotagestor.plugins.RawBodyKey
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class UserController(private val userService: UserService) {
    private val internalMsgError = "Internal server error"

    suspend fun login(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val result = userService.login(rawBody)
            call.respond(result.status, result.data)
        } catch (e: Exception) {
            println("Error in login controller: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Internal server error")
            )
        }
    }

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = userService.createUser(rawBody)
            call.respond(serviceResult.status, serviceResult.data)

        } catch (e: Exception) {
            println("Error in create controller: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
        }
    }

}