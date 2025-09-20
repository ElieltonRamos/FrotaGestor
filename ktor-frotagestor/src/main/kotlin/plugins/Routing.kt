package com.frotagestor.plugins

import com.frotagestor.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        staticResources("/", "static") {
            defaultResource("static/index.html")
        }

        userRoutes()
    }
}
