package com.frotagestor

import com.frotagestor.accurate_gt_06.startTcpServerGt06
import com.frotagestor.database.DatabaseFactory
import com.frotagestor.plugins.configureRouting
import io.ktor.server.application.*
import com.frotagestor.plugins.*
import com.frotagestor.protocols_devices_gps.suntech.startTcpServerSuntech
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
    launch {
//        startTcpServerGt06()
        startTcpServerSuntech()
    }
}

