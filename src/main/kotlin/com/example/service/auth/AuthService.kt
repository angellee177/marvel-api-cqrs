package com.example.service.auth

import com.example.models.Login
import com.example.models.ProfileType
import com.example.models.Register
import com.example.service.userProfile.ProfileService
import com.example.utils.JWT
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuthService(
    private val profileService: ProfileService,
    private val jwt: JWT = JWT
) {
    private val logger: Logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun register(creds: Register) {
        logger.info("Attempting to register user with email: ${creds.email}")
        try {
            val hashedPassword = BCrypt.hashpw(creds.password, BCrypt.gensalt())
            profileService.registerProfile(
                creds.name,
                creds.email,
                hashedPassword
            )
            logger.info("Registration successful for email: ${creds.email}")
        } catch (e: Exception) {
            logger.error("Registration failed for email: ${creds.email}", e)
            throw e
        }
    }

    suspend fun login(creds: Login): String? {
        logger.info("Attempting login for email: ${creds.email}")
        try {
            val profile = profileService.getProfileByEmail(creds.email)
                ?: throw IllegalArgumentException("Invalid Credentials")

            if (!BCrypt.checkpw(creds.password, profile.password)) {
                logger.warn("Invalid password for email: ${creds.email}")
                throw IllegalArgumentException("Invalid Credentials")
            }

            val token = jwt.createJwtToken(profile.email)
            logger.info("Login successful for email: ${creds.email}")
            return token
        } catch (e: Exception) {
            logger.error("Login failed for email: ${creds.email}", e)
            throw e
        }
    }

    suspend fun getProfileByToken(token: String): ProfileType {
        logger.info("Attempting to retrieve profile using token")
        try {
            val email = jwt.verifyTokenAndGetEmail(token)
                ?: throw IllegalArgumentException("Invalid token: Unable to extract email")

            val profile: ProfileType = profileService.getProfileByEmail(email)
                ?: throw IllegalArgumentException("Profile not found for email: $email")

            logger.info("Profile retrieved successfully for email: $email")
            return profile
        } catch (e: Exception) {
            logger.error("Failed to retrieve profile using token", e)
            throw e
        }
    }
}
