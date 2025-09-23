package com.frotagestor.routes

import com.frotagestor.controllers.UserController
import com.frotagestor.services.UserService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.userRoutes() {
    val controller = UserController(UserService())

    route("users") {
        post("login") { controller.login(call) } // ❌ sem JWT (qualquer um pode logar)

        authenticate("auth-jwt") {
            post { controller.create(call) }
            patch("{id}") { controller.update(call) }
            delete("{id}") { controller.delete(call) }
        }
    }
}
