package com.frotagestor.routes

import com.frotagestor.controllers.ReportsController
import com.frotagestor.services.ReportsService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.reportsRoutes() {
    val controller = ReportsController(ReportsService())

    authenticate("auth-jwt") {
        route("reports") {
            get("vehicles") { controller.getReportVehicles(call) }
            get("trips") { controller.getTripReport(call) }
            get("expenses") { controller.getReportExpenses(call) }
            get("drivers") { controller.getReportDriver(call) }
        }
    }
}
