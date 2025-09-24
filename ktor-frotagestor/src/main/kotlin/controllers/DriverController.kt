package com.frotagestor.controllers

import com.frotagestor.database.models.DriversTable
import com.frotagestor.interfaces.DriverStatus
import com.frotagestor.services.DriverService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo
import org.jetbrains.exposed.sql.SortOrder

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
            // 📌 Query params básicos de paginação/ordenação
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val sortByParam = call.request.queryParameters["sortBy"] ?: "id"
            val orderParam = call.request.queryParameters["order"] ?: "asc"

            // 📌 Query params de filtro
            val idFilter = call.request.queryParameters["id"]?.toIntOrNull()
            val nameFilter = call.request.queryParameters["name"]
            val cpfFilter = call.request.queryParameters["cpf"]
            val cnhFilter = call.request.queryParameters["cnh"]
            val cnhCategoryFilter = call.request.queryParameters["cnhCategory"]
            val cnhExpirationFilter = call.request.queryParameters["cnhExpiration"]?.let {
                runCatching { kotlinx.datetime.LocalDate.parse(it) }.getOrNull()
            }
            val phoneFilter = call.request.queryParameters["phone"]
            val emailFilter = call.request.queryParameters["email"]
            val statusFilter = call.request.queryParameters["status"]?.let {
                runCatching { DriverStatus.valueOf(it.uppercase()) }.getOrNull()
            }

            // 📌 Mapeia string -> coluna do banco
            val sortByColumn = when (sortByParam.lowercase()) {
                "name" -> DriversTable.name
                "cpf" -> DriversTable.cpf
                "cnh" -> DriversTable.cnh
                "cnhcategory" -> DriversTable.cnhCategory
                "cnhexpiration" -> DriversTable.cnhExpiration
                "phone" -> DriversTable.phone
                "email" -> DriversTable.email
                "status" -> DriversTable.status
                else -> DriversTable.id
            }

            val sortOrder = if (orderParam.equals("desc", ignoreCase = true)) {
                SortOrder.DESC
            } else {
                SortOrder.ASC
            }

            // 📌 Chama o service passando todos os filtros
            val serviceResult = driverService.getAllDrivers(
                page = page,
                limit = limit,
                sortBy = sortByColumn,
                sortOrder = sortOrder,
                idFilter = idFilter,
                nameFilter = nameFilter,
                cpfFilter = cpfFilter,
                cnhFilter = cnhFilter,
                cnhCategoryFilter = cnhCategoryFilter,
                cnhExpirationFilter = cnhExpirationFilter,
                phoneFilter = phoneFilter,
                emailFilter = emailFilter,
                statusFilter = statusFilter
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll driver route: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
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
