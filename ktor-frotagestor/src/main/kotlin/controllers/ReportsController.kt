package com.frotagestor.controllers

import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.VehicleReport
import com.frotagestor.services.ReportsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.datetime.LocalDate
import kotlin.system.measureTimeMillis

class ReportsController(private val reportsService: ReportsService) {
    suspend fun getReportVehicles(call: ApplicationCall) {
        try {
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]
            val startDate = startDateParam?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }
            val endDate = endDateParam?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }

            var serviceResult: ServiceResponse<VehicleReport>
            val timeMillis = measureTimeMillis {
                serviceResult = reportsService.getReportVehicles(startDate, endDate)
            }

            println("⏱ Vehicle report service execution time: ${timeMillis}ms")

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("❌ Error in getReport vehicle route: ${e.stackTraceToString()}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Erro interno ao gerar relatório de veículos.")
            )
        }
    }
}