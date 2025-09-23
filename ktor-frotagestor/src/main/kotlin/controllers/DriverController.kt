package com.frotagestor.controllers

import com.frotagestor.services.DriverService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo

class DriverController(private val driverService: DriverService) {
    private val internalMsgError = "Internal server error"

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = driverService.createDriver(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create driver route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun update(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = driverService.updateDriver(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in update driver route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getAll(call: ApplicationCall) {
        try {
            val serviceResult = driverService.getAllDrivers()
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll driver route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getById(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = driverService.findDriverById(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getById driver route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun softDelete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = driverService.softDeleteDriver(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in softDelete driver route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}
