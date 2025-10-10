package com.frotagestor.routes

import com.frotagestor.controllers.VehicleController
import com.frotagestor.services.VehicleService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.vehicleRoutes() {
    val controller = VehicleController(VehicleService())

    authenticate("auth-jwt") {
        route("vehicles") {
            get { controller.getAll(call) }
            post { controller.create(call) }
            get("indicators") { controller.getIndicators(call) }
            get("{id}") { controller.getById(call) }
            patch("{id}") { controller.update(call) }
            delete("{id}") { controller.softDelete(call) }
            get("{id}/trips") { controller.getTripsByVehicle(call) }
            get("{id}/expenses") { controller.getExpensesByVehicle(call) }
            get("{id}/top-driver") { controller.getTopDriver(call) }
        }
    }
}
