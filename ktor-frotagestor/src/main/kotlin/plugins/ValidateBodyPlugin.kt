package com.frotagestor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.AttributeKey

val RawBodyKey = AttributeKey<String>("RawBody")

fun Application.configureValidateBody() {
    install(createApplicationPlugin(name = "ValidateBodyPlugin") {
        onCall { call ->
            if (call.request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
                val rawBody = call.receiveText().trim()

                if (rawBody.isEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Corpo da requisição está vazio")
                    )
                    return@onCall
                }

                call.attributes.put(RawBodyKey, rawBody)
            }
        }
    })
}
