package com.frotagestor.routes

import com.frotagestor.controllers.GpsCommandController
import com.frotagestor.services.GpsCommandService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.gpsCommandRoutes() {
    val controller = GpsCommandController(GpsCommandService())

    authenticate("auth-jwt") {
        route("gps-commands") {

            // 1. ROTAS COM PARÂMETROS (DEVEM VIR PRIMEIRO)
            post("{deviceId}") { controller.sendCommand(call) }
            post("{deviceId}/location") { controller.requestLocation(call) }
            post("{deviceId}/immobilizer") { controller.controlImmobilizer(call) }
            post("{deviceId}/report-intervals") { controller.setReportIntervals(call) }
            post("{deviceId}/reboot") { controller.rebootDevice(call) }
            post("{deviceId}/server") { controller.configureServer(call) }

            // 2. ROTAS ESTÁTICAS (DEPOIS)
            get("connected") { controller.getConnectedDevices(call) }
        }
    }
}