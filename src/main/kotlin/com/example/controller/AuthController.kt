package com.example.controller

import com.example.models.Login
import com.example.models.Register
import com.example.service.userProfile.ProfileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.mindrot.jbcrypt.BCrypt
import com.example.utils.JWT as jwt

class AuthController(
    private val profileService: ProfileService,
) {
    suspend fun register(call: ApplicationCall) {
        try {
            val creds = call.receive<Register>()
            val hashedPassword = BCrypt.hashpw(creds.password, BCrypt.gensalt())
            profileService.registerProfile(
                creds.name,
                creds.email,
                hashedPassword
            )

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
            val profile = profileService.getProfileByEmail(creds.email)

            if (profile == null || !BCrypt.checkpw(creds.password, profile.password)) {
                call.respondText("Invalid Credentials", status = HttpStatusCode.Unauthorized)
                return
            }

            val token = jwt.createJwtToken(profile.email)
            call.respond(mapOf("token" to token))
        } catch (e: Exception) {
            call.respondText(
                "Login failed: ${e.localizedMessage}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    suspend fun myProfile(call: ApplicationCall) {
        try {
            // Extract the token from the Authorization header
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                ?: throw IllegalArgumentException("Authorization token is missing")

            // Decode the token and extract the subject (email)
            val email = jwt.verifyTokenAndGetEmail(token)
                ?: throw IllegalArgumentException("Invalid token: Unable to extract email")

            // Fetch the user's profile using the email
            val profile = profileService.getProfileByEmail(email)
                ?: throw IllegalArgumentException("Profile not found for email: $email")

            // Respond with the user's profile
            call.respond(profile)

        } catch (e: Exception) {
            // Respond with an error message if something goes wrong
            call.respondText(
                "Failed to fetch profile: ${e.localizedMessage}",
                status = HttpStatusCode.Unauthorized
            )
        }
    }
}