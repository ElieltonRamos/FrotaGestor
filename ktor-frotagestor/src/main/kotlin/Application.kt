package com.frotagestor

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.plugins.configureRouting
import io.ktor.server.application.*
import com.frotagestor.plugins.*
import com.frotagestor.protocols_devices_gps.gt06.startTcpServerGT06
import com.frotagestor.protocols_devices_gps.suntech.startTcpServerSuntech
import com.frotagestor.services.DailyTripService
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    DatabaseFactory.init()
    configureCors()
    configureValidateBody()
    configureRouting()

    val dailyTripService = DailyTripService()
    dailyTripService.startDailyCleanup(this)

    launch { startTcpServerSuntech(dailyTripService) }
    launch { startTcpServerGT06(dailyTripService) }
}

