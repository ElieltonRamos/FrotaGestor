package com.frotagestor.controllers

import com.frotagestor.database.models.UsersTable
import com.frotagestor.services.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import com.frotagestor.plugins.RawBodyKey
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.SortOrder

class UserController(private val userService: UserService) {
    private val internalMsgError = "Internal server error"

    suspend fun login(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val result = userService.login(rawBody)
            call.respond(result.status, result.data)
        } catch (e: Exception) {
            println("Error in login user route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = userService.createUser(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create user route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun update(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = userService.updateUser(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in update user route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun delete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = userService.deleteUser(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in delete user route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getAll(call: ApplicationCall) {
        try {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val sortByParam = call.request.queryParameters["sortBy"] ?: "id"
            val orderParam = call.request.queryParameters["order"] ?: "asc"

            val idFilter = call.request.queryParameters["id"]?.toIntOrNull()
            val usernameFilter = call.request.queryParameters["username"]
            val roleFilter = call.request.queryParameters["role"]

            val sortByColumn = when (sortByParam.lowercase()) {
                "username" -> UsersTable.username
                "role" -> UsersTable.role
                else -> UsersTable.id
            }

            val sortOrder = if (orderParam.equals("desc", ignoreCase = true)) {
                SortOrder.DESC
            } else {
                SortOrder.ASC
            }

            val serviceResult = userService.getAllUsers(
                page = page,
                limit = limit,
                sortBy = sortByColumn,
                sortOrder = sortOrder,
                idFilter = idFilter,
                usernameFilter = usernameFilter,
                roleFilter = roleFilter
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll user route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getById(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = userService.findUserById(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getById user route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getIndicators(call: ApplicationCall) {
        try {
            val serviceResult = userService.getIndicatorsUsers()
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getIndicators user route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}
