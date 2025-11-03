package com.frotagestor.controllers

import com.frotagestor.plugins.RawBodyKey
import com.frotagestor.services.GpsDeviceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.datetime.LocalDateTime

class GpsDeviceController(private val gpsDeviceService: GpsDeviceService) {

    private val internalMsgError = "Internal server error"

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = gpsDeviceService.createGpsDevice(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create GPS device route: ${e.message}")
            e.printStackTrace()
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
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun delete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = gpsDeviceService.deleteGpsDevice(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in delete GPS device route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getAll(call: ApplicationCall) {
        try {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val vehicleIdFilter = call.request.queryParameters["vehicleId"]?.toIntOrNull()
            val imeiFilter = call.request.queryParameters["imei"]

            val serviceResult = gpsDeviceService.getAllGpsDevices(
                page = page,
                limit = limit,
                vehicleIdFilter = vehicleIdFilter,
                imeiFilter = imeiFilter
            )
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll GPS devices route: ${e.message}")
            e.printStackTrace()
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
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun findGpsDeviceByVehicleId(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = gpsDeviceService.findGpsDeviceByVehicleId(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in findGpsDeviceByVehicleId GPS device route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getHistoryByVehicle(call: ApplicationCall) {
        try {
            val vehicleId = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            // Lê query parameters opcionais start e end
            val startParam = call.request.queryParameters["startDate"]
            val endParam = call.request.queryParameters["endDate"]

            val startDate: LocalDateTime? = startParam?.let { LocalDateTime.parse(it) }
            val endDate: LocalDateTime? = endParam?.let { LocalDateTime.parse(it) }

            val serviceResult = gpsDeviceService.getHistoryByVehicle(
                vehicleId = vehicleId,
                startDate = startDate,
                endDate = endDate
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getHistoryByVehicle route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}