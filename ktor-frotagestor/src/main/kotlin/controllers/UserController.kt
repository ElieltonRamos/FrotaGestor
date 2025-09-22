package com.frotagestor.controllers

import com.frotagestor.services.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import com.frotagestor.plugins.RawBodyKey
import io.ktor.server.response.respond

class UserController(private val userService: UserService) {
    private val internalMsgError = "Internal server error"

    suspend fun login(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val result = userService.login(rawBody)
            call.respond(result.status, result.data)
        } catch (e: Exception) {
            println("Error in login user route: ${e.message}")
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
            println("Error in create user route: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
        }
    }

    suspend fun update(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]

            val id = call.parameters["id"]?.toIntOrNull() ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'id' inválido ou ausente")
            )

            val serviceResult = userService.updateUser(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)

        } catch (e: Exception) {
            println("Error in update user route: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
        }
    }

    suspend fun delete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull() ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'id' inválido ou ausente")
            )

            val serviceResult = userService.deleteUser(id)
            call.respond(serviceResult.status, serviceResult.data)

        } catch (e: Exception) {
            println("Error in delete user route: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
        }
    }

}