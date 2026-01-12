package com.frotagestor.routes

import com.frotagestor.controllers.SubfleetController
import com.frotagestor.services.SubfleetService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.subfleetRoutes() {
    val controller = SubfleetController(SubfleetService())

    authenticate("auth-jwt") {
        route("subfleets") {
            get { controller.getAll(call) }
            post { controller.create(call) }
            get("{id}") { controller.getById(call) }
            patch("{id}") { controller.update(call) }
            delete("{id}") { controller.delete(call) }
            get("{id}/report") { controller.getReport(call) }
            get("{id}/vehicles") { controller.getVehiclesBySubfleet(call) }
        }
    }
}
