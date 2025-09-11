package com.redenorte

import com.redenorte.routes.configureRouting
import io.ktor.server.application.*
import com.redenorte.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()  // JSON
    configureSecurity()       // JWT
    configureRouting()        // rotas
}

