package com.frotagestor.routes

import com.frotagestor.controllers.GpsDeviceController
import com.frotagestor.services.GpsDeviceService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.gpsDeviceRoutes() {
    val controller = GpsDeviceController(GpsDeviceService())

    authenticate("auth-jwt") {
        route("gps-devices") {
            get { controller.getAll(call) }
            post { controller.create(call) }
            get("{id}") { controller.getById(call) }
            patch("{id}") { controller.update(call) }
        }
    }
}
