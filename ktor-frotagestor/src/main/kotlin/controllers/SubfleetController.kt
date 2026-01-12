package com.frotagestor.controllers

import com.frotagestor.interfaces.SubfleetStatus
import com.frotagestor.services.SubfleetService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

class SubfleetController(private val subfleetService: SubfleetService) {
    private val internalMsgError = "Internal server error"

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
     * PUT /subfleets/:id - Atualizar subfrota
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
     * GET /subfleets - Listar subfrotas com filtros
     * Query params:
     * - status: ACTIVE | INACTIVE
     * - parentId: ID da subfrota pai
     * - managerUserId: ID do usuário gerente
     */
    /**
     * GET /subfleets - Listar subfrotas com filtros e paginação
     * Query params:
     * - page: número da página (padrão: 1)
     * - limit: itens por página (padrão: 10)
     * - status: ACTIVE | INACTIVE
     * - parentId: ID da subfrota pai
     * - managerUserId: ID do usuário gerente
     */
    suspend fun getAll(call: ApplicationCall) {
        try {
            // Paginação
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            // Filtros
            val statusFilter = call.request.queryParameters["status"]?.let {
                runCatching { SubfleetStatus.valueOf(it.uppercase()) }.getOrNull()
            }

            val parentIdFilter = call.request.queryParameters["parentId"]?.toIntOrNull()
            val managerUserIdFilter = call.request.queryParameters["managerUserId"]?.toIntOrNull()

            val serviceResult = subfleetService.getAllSubfleets(
                page = page,
                limit = limit,
                statusFilter = statusFilter,
                parentIdFilter = parentIdFilter,
                managerUserIdFilter = managerUserIdFilter
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll subfleets route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    /**
     * GET /subfleets/:id - Buscar subfrota por ID
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
     * DELETE /subfleets/:id - Deletar subfrota
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

    /**
     * GET /subfleets/:id/report - Relatório da subfrota
     */
    suspend fun getReport(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val serviceResult = subfleetService.getSubfleetReport(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getReport subfleet route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    /**
     * GET /subfleets/:id/vehicles - Listar veículos de uma subfrota (delegado)
     * Nota: Este método delega para VehicleController.getAll com filtro
     */
    suspend fun getVehiclesBySubfleet(call: ApplicationCall) {
        try {
            val subfleetId = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            // Redirecionar para /vehicles?subfleetId=X
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "message" to "Use GET /vehicles?subfleetId=$subfleetId para listar veículos desta subfrota"
                )
            )
        } catch (e: Exception) {
            println("Error in getVehiclesBySubfleet route: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}
