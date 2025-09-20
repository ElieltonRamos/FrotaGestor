package com.redenorte.routes

import com.redenorte.controllers.UserController
import com.redenorte.services.UserService
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.util.*

fun Route.userRoutes() {
    val controller = UserController(UserService())

    post("/login") { controller.login(call) }
//    post("/users") { controller.create(call) }
}
