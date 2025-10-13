package com.frotagestor.controllers

import com.frotagestor.plugins.RawBodyKey
import com.frotagestor.services.GpsDeviceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

class GpsDeviceController(private val gpsDeviceService: GpsDeviceService) {

    private val internalMsgError = "Internal server error"

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = gpsDeviceService.createGpsDevice(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create GPS device route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun update(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))
            val rawBody = call.attributes[RawBodyKey]

            val serviceResult = gpsDeviceService.updateGpsDevice(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in update GPS device route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getAll(call: ApplicationCall) {
        try {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val vehicleIdFilter = call.request.queryParameters["vehicleId"]?.toIntOrNull()

            val serviceResult = gpsDeviceService.getAllGpsDevices(
                page = page,
                limit = limit,
                vehicleIdFilter = vehicleIdFilter
            )
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll GPS devices route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getById(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = gpsDeviceService.findGpsDeviceById(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getById GPS device route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}
