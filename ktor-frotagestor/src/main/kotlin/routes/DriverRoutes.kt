package com.frotagestor.routes

import com.frotagestor.controllers.DriverController
import com.frotagestor.services.DriverService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.driverRoutes() {
    val controller = DriverController(DriverService())

    authenticate("auth-jwt") {
        route("drivers") {
            get { controller.getAll(call) }
            post { controller.create(call) }
            get("indicators") { controller.getIndicators(call) }
            get("{id}") { controller.getById(call) }
            patch("{id}") { controller.update(call) }
            delete("{id}") { controller.softDelete(call) }
            get("{id}/vehicles") { controller.getVehiclesByDriver(call) }
            get("{id}/expenses") { controller.getExpensesByDriver(call) }
        }
    }
}
