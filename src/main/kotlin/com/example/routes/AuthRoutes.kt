package com.example.routes

import com.example.controller.AuthController
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.authRoutes(authController: AuthController) {
    route("/auth") {
        // Public routes (do not require authentication)
        post("/register") {
            authController.register(call)
        }
        post("/login") {
            authController.login(call)
        }

        // Authenticated routes (requires token)
        authenticate {
            get("/my-profile") {
                authController.myProfile(call)
            }
        }
    }
}
