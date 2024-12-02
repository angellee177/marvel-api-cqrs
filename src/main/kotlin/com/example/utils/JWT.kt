package com.example.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object JWT {
    // Create a logger for this object
    private val logger: Logger = LoggerFactory.getLogger(JWT::class.java)

    private val appConfig: ApplicationConfig = HoconApplicationConfig(ConfigFactory.load("application.conf"))

    private val jwtSecret = appConfig.property("jwt.secret").getString()
    private val jwtIssuer = appConfig.property("jwt.issuer").getString()
    private val jwtAudience = appConfig.property("jwt.audience").getString()
    val jwtRealm = appConfig.property("jwt.realm").getString()

    private const val validityInMs = 36_000_00 * 1     // 1 hour

    // Log JWT token creation
    fun createJwtToken(email: String): String? {
        logger.info("Creating JWT token for email: $email")  // Info level log

        try {
            return JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("email", email)
                .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
                .sign(Algorithm.HMAC256(jwtSecret))
        } catch (e: Exception) {
            logger.error("Error creating JWT token for email: $email", e)  // Error level log
            throw e  // Propagate the exception
        }
    }

    // Log JWT verification setup
    val jwtVerifier: JWTVerifier = JWT
        .require(Algorithm.HMAC256(jwtSecret))
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .build()

    // Log when JWT verification is used and return email after decoding
    fun verifyTokenAndGetEmail(token: String): String? {
        logger.info("Verifying JWT token: $token")  // Info level log
        try {
            // Verify the token
            val decodedJWT = jwtVerifier.verify(token)

            // Extract the email from the decoded JWT
            val email = decodedJWT.getClaim("email").asString()

            logger.info("Token verified successfully, email extracted: $email") // Info level log
            return email

        } catch (e: Exception) {
            logger.error("Error verifying JWT token: $token", e)  // Error level log
            throw e  // Propagate the exception
        }
    }
}
