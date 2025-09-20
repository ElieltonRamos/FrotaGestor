package com.frotagestor.routes

import com.frotagestor.controllers.UserController
import com.frotagestor.services.UserService
import io.ktor.server.routing.*

fun Route.userRoutes() {
    val controller = UserController(UserService())

    post("/login") { controller.login(call) }
//    post("/users") { controller.create(call) }
}
