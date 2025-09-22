package com.frotagestor

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.plugins.configureRouting
import io.ktor.server.application.*
import com.frotagestor.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()  // JSON
    configureSecurity()       // JWT
    DatabaseFactory.init()
    install(ValidateBodyPlugin)
    configureRouting()        // rotas
}

