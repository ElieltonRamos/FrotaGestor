package com.redenorte.routes

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        userRoutes()

        authenticate("auth-jwt") {
            get("/secure") {
                call.respondText("Rota protegida ✅")
            }
        }
    }
}
