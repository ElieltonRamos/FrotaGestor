package com.frotagestor.routes

import com.frotagestor.controllers.TripController
import com.frotagestor.services.TripService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.tripRoutes() {
    val controller = TripController(TripService())

    authenticate("auth-jwt") {
        route("trips") {
            get { controller.getAll(call) }
            post { controller.create(call) }
            get("{id}") { controller.getById(call) }
            patch("{id}") { controller.update(call) }
            delete("{id}") { controller.delete(call) }
        }
    }
}
