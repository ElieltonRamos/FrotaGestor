package com.frotagestor.controllers

import com.frotagestor.interfaces.CommandRequest
import com.frotagestor.plugins.RawBodyKey
import com.frotagestor.services.GpsDeviceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
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
            // 1. Validação do ID do veículo
            val vehicleId = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            // 2. Parâmetros de paginação
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                ?.coerceIn(1, 100) ?: 20  // Máximo 100 por página

            // 3. Parâmetros de data (opcional)
            val startDate: LocalDateTime? = call.request.queryParameters["startDate"]?.let { param ->
                try { LocalDateTime.parse(param) } catch (e: Exception) {
                    return call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Formato de 'startDate' inválido. Use ISO: YYYY-MM-DDTHH:MM:SS")
                    )
                }
            }

            val endDate: LocalDateTime? = call.request.queryParameters["endDate"]?.let { param ->
                try { LocalDateTime.parse(param) } catch (e: Exception) {
                    return call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Formato de 'endDate' inválido. Use ISO: YYYY-MM-DDTHH:MM:SS")
                    )
                }
            }

            // 4. Chama o serviço
            val serviceResult = gpsDeviceService.getHistoryByVehicle(
                vehicleId = vehicleId,
                startDate = startDate,
                endDate = endDate,
                page = page,
                limit = limit
            )

            // 5. Responde com status e corpo
            call.respond(serviceResult.status, serviceResult.data)

        } catch (e: Exception) {
            // 6. Log de erro (produção: use logger)
            println("Error in getHistoryByVehicle route: ${e.message}")
            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Erro interno no servidor")
            )
        }
    }

    suspend fun sendCommand(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = gpsDeviceService.sendCommandDevice(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in sendCommand route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}