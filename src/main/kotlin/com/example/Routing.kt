package com.example

import com.example.controller.AuthController
import com.example.controller.UserController
import com.example.routes.authRoutes
import com.example.routes.userRoutes
import com.example.service.auth.AuthService
import com.example.service.userProfile.ProfileService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val authController = AuthController(
        AuthService(ProfileService())
    )
    val userController = UserController(ProfileService())

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        authRoutes(authController)
        userRoutes(userController)
    }
}
