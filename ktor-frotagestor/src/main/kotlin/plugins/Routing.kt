package com.frotagestor.plugins

import com.frotagestor.routes.driverRoutes
import com.frotagestor.routes.expenseRoutes
import com.frotagestor.routes.gpsDeviceRoutes
import com.frotagestor.routes.reportsRoutes
import com.frotagestor.routes.statusRoutes
import com.frotagestor.routes.subfleetRoutes
import com.frotagestor.routes.tripRoutes
import com.frotagestor.routes.userRoutes
import com.frotagestor.routes.vehicleRoutes
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.time.Instant

fun Application.configureRouting() {
    routing {
        val appStartTime = Instant.now()
        staticResources("/", "static") {
            defaultResource("static/index.html")
        }
        statusRoutes(appStartTime)
        userRoutes()
        driverRoutes()
        vehicleRoutes()
        tripRoutes()
        expenseRoutes()
        reportsRoutes()
        gpsDeviceRoutes()
        subfleetRoutes()
    }
}
