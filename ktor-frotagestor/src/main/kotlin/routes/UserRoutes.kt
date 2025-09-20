package com.redenorte.routes

import com.redenorte.models.LoginRequest
import com.redenorte.models.LoginResponse
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.redenorte.controllers.UserController
import com.redenorte.services.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.util.*

fun Route.userRoutes() {
    val controller = UserController(UserService())

    post("/login") { controller.login(call) }
    post("/users") { controller.create(call) }
}
