package com.frotagestor.controllers

import com.frotagestor.plugins.RawBodyKey
import com.frotagestor.services.GpsCommandService
import com.frotagestor.interfaces.CommandRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json

class GpsCommandController(private val gpsCommandService: GpsCommandService) {

    private val internalMsgError = "Internal server error"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendCommand(call: ApplicationCall) {
        try {
            val deviceId = call.parameters["deviceId"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'deviceId' ausente")
            )

            val rawBody = call.attributes[RawBodyKey]

            // Tenta fazer o parse do CommandRequest
            val request = try {
                json.decodeFromString<CommandRequest>(rawBody)
            } catch (e: Exception) {
                // Erro de parsing JSON (malformado, campos faltando, tipos errados, etc)
                println("Invalid JSON in sendCommand route: ${e.message}")
                return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Corpo da requisição inválido: JSON malformado ou campos incorretos")
                )
            }

            val serviceResult = gpsCommandService.sendCommand(deviceId, request)
            call.respond(serviceResult.status, serviceResult.data)

        } catch (e: Exception) {
            // Qualquer outro erro (banco, conexão, etc)
            println("Error in sendCommand route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun requestLocation(call: ApplicationCall) {
        try {
            val deviceId = call.parameters["deviceId"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'deviceId' ausente")
            )

            val serviceResult = gpsCommandService.requestLocation(deviceId)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in requestLocation route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun controlImmobilizer(call: ApplicationCall) {
        try {
            val deviceId = call.parameters["deviceId"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'deviceId' ausente")
            )
            val enableParam = call.request.queryParameters["enable"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'enable' (true/false) é obrigatório")
            )
            val enable = enableParam.toBooleanStrictOrNull() ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'enable' deve ser true ou false")
            )

            val serviceResult = gpsCommandService.controlImmobilizer(deviceId, enable)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in controlImmobilizer route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun setReportIntervals(call: ApplicationCall) {
        try {
            val deviceId = call.parameters["deviceId"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'deviceId' ausente")
            )
            val driving = call.request.queryParameters["driving"]?.toIntOrNull() ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'driving' inválido ou ausente")
            )
            val parking = call.request.queryParameters["parking"]?.toIntOrNull() ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'parking' inválido ou ausente")
            )

            val serviceResult = gpsCommandService.setReportIntervals(deviceId, driving, parking)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in setReportIntervals route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun rebootDevice(call: ApplicationCall) {
        try {
            val deviceId = call.parameters["deviceId"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'deviceId' ausente")
            )

            val serviceResult = gpsCommandService.rebootDevice(deviceId)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in rebootDevice route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun configureServer(call: ApplicationCall) {
        try {
            val deviceId = call.parameters["deviceId"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'deviceId' ausente")
            )
            val serverIp = call.request.queryParameters["serverIp"] ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'serverIp' ausente")
            )
            val serverPort = call.request.queryParameters["serverPort"]?.toIntOrNull() ?: return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Parâmetro 'serverPort' inválido ou ausente")
            )
            val protocolType = call.request.queryParameters["protocolType"] ?: "T"

            val serviceResult = gpsCommandService.configureServer(deviceId, serverIp, serverPort, protocolType)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in configureServer route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getConnectedDevices(call: ApplicationCall) {
        try {
            val serviceResult = gpsCommandService.getConnectedDevices()
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getConnectedDevices route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}