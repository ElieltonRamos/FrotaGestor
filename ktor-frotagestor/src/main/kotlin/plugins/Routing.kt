package com.frotagestor.plugins

import com.frotagestor.routes.driverRoutes
import com.frotagestor.routes.expenseRoutes
import com.frotagestor.routes.tripRoutes
import com.frotagestor.routes.userRoutes
import com.frotagestor.routes.vehicleRoutes
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        staticResources("/", "static") {
            defaultResource("static/index.html")
        }
        userRoutes()
        driverRoutes()
        vehicleRoutes()
        tripRoutes()
        expenseRoutes()
    }
}
