package com.frotagestor.routes

import com.frotagestor.controllers.UserController
import com.frotagestor.services.UserService
import io.ktor.server.routing.*

fun Route.userRoutes() {
    val controller = UserController(UserService())

    route("users") {
        post("login") { controller.login(call) }
        post("create") { controller.create(call) }
        patch("update/{id}") { controller.update(call) }
        delete("delete/{id}") { controller.delete(call) }
    }
}
