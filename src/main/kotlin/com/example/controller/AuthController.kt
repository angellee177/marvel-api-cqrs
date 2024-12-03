package com.example.controller

import com.example.models.Login
import com.example.models.Register
import com.example.service.auth.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class AuthController(
    private val authService: AuthService
) {
    suspend fun register(call: ApplicationCall) {
        try {
            val creds = call.receive<Register>()
            authService.register(creds)
            call.respondText("Registration Successful!", status = HttpStatusCode.Created)
        } catch (e: Exception) {
            call.respondText(
                "Failed to register user: ${e.localizedMessage}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    suspend fun login(call: ApplicationCall) {
        try {
            val creds = call.receive<Login>()
            val token = authService.login(creds)
            call.respond(mapOf("token" to token))
        } catch (e: IllegalArgumentException) {
            call.respondText(
                e.localizedMessage,
                status = HttpStatusCode.Unauthorized
            )
        } catch (e: Exception) {
            call.respondText(
                "Login failed: ${e.localizedMessage}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    suspend fun myProfile(call: ApplicationCall) {
        try {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                ?: throw IllegalArgumentException("Authorization token is missing")
            val profile = authService.getProfileByToken(token)
            call.respond(profile)
        } catch (e: IllegalArgumentException) {
            call.respondText(
                e.localizedMessage,
                status = HttpStatusCode.Unauthorized
            )
        } catch (e: Exception) {
            call.respondText(
                "Failed to fetch profile: ${e.localizedMessage}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}
