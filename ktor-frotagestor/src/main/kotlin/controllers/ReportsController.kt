package com.frotagestor.controllers

import com.frotagestor.interfaces.ExpenseReport
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.TripReport
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

    suspend fun getTripReport(call: ApplicationCall) {
        try {
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]
            val startDate = startDateParam?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }
            val endDate = endDateParam?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }

            var serviceResult: ServiceResponse<TripReport>
            val timeMillis = measureTimeMillis {
                serviceResult = reportsService.getTripReport(startDate, endDate)
            }

            println("⏱ Trip report service execution time: ${timeMillis}ms")

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("❌ Error in getTripReport route: ${e.stackTraceToString()}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Erro interno ao gerar relatório de viagens.")
            )
        }
    }

    suspend fun getReportExpenses(call: ApplicationCall) {
        try {
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]
            val startDate = startDateParam?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }
            val endDate = endDateParam?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }

            var serviceResult: ServiceResponse<ExpenseReport>
            val timeMillis = measureTimeMillis {
                serviceResult = reportsService.getReportExpenses(startDate, endDate)
            }

            println("⏱ Expense report service execution time: ${timeMillis}ms")

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("❌ Error in getReportExpenses route: ${e.stackTraceToString()}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Erro interno ao gerar relatório de despesas.")
            )
        }
    }
}