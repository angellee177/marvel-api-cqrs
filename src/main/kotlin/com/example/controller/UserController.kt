package com.example.controller

import com.example.headerData
import com.example.models.ProfileType
import com.example.service.userProfile.ProfileService
import com.example.utils.JWT
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

class UserController(
    private val profileService: ProfileService,
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    suspend fun getAllUsers(call: ApplicationCall) {
        try {
            // Extract the token from the Authorization header
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                ?: throw IllegalArgumentException("Missing JWT token")

            // Log and verify the JWT token
            logger.info("Received token for verification: $token")
            JWT.verifyTokenAndGetEmail(token)  // This will log and verify the JWT token

            // Log the JWT token details using the helper function from Security.kt
            call.headerData(logger)

            // If the token is valid, fetch and return users
            val users: List<ProfileType> = profileService.getAllUsers() // Call to the service layer
            call.respond(users) // Respond with the list of users
        } catch (e: Exception) {
            logger.error("Failed to fetch users: ${e.localizedMessage}", e)
            call.respondText(
                "Failed to fetch users: ${e.localizedMessage}",
                status = io.ktor.http.HttpStatusCode.InternalServerError
            )
        }
    }
}
