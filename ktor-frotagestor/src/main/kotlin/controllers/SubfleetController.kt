package com.frotagestor.controllers

import com.frotagestor.services.SubfleetService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import com.frotagestor.plugins.RawBodyKey

class SubfleetController(private val subfleetService: SubfleetService) {
    private val internalMsgError = "Internal server error"

    /**
     * GET /subfleets/indicators - Dashboard indicators
     */
    suspend fun getIndicators(call: ApplicationCall) {
        try {
            val serviceResult = subfleetService.getIndicators()
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getIndicators subfleet route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    /**
     * POST /subfleets - Criar nova subfrota
     */
    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = subfleetService.createSubfleet(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create subfleet route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    /**
     * PATCH /subfleets/:id - Atualizar subfrota
     */
    suspend fun update(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val serviceResult = subfleetService.updateSubfleet(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in update subfleet route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    /**
     * GET /subfleets - Listar com paginação e filtros simples
     * Query params: page, limit, name, description
     */
    suspend fun getAll(call: ApplicationCall) {
        try {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val nameFilter = call.request.queryParameters["name"]
            val descriptionFilter = call.request.queryParameters["description"]

            val serviceResult = subfleetService.getAllSubfleets(
                page = page,
                limit = limit,
                nameFilter = nameFilter,
                descriptionFilter = descriptionFilter
            )
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll subfleets route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    /**
     * GET /subfleets/:id - Buscar por ID
     */
    suspend fun getById(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val serviceResult = subfleetService.findSubfleetById(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getById subfleet route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    /**
     * DELETE /subfleets/:id
     */
    suspend fun delete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val serviceResult = subfleetService.deleteSubfleet(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in delete subfleet route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getVehiclesBySubfleet(call: ApplicationCall) {
        try {
            val subfleetId = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            val serviceResult = subfleetService.getVehiclesBySubfleet(subfleetId, page, limit)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getVehiclesBySubfleet: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

}
