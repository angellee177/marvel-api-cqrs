package com.example.routes

import io.ktor.server.routing.*

import com.example.controller.UserController
import io.ktor.server.auth.*

fun Route.userRoutes(userController: UserController) {
    authenticate {
        route("/users") {
            get {
                userController.getAllUsers(call) // Call the controller function to get all users
            }
        }
    }
}
