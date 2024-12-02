package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import javax.naming.AuthenticationException

fun Application.configureSecurity() {
    // Create a logger for the module
    val logger = LoggerFactory.getLogger("ApplicationModule")

    // Load the application configuration
    val appConfig = HoconApplicationConfig(ConfigFactory.load())
    logger.info("Loaded application.conf successfully")

    val jwtConfig = appConfig.config("jwt")  // Read the jwt section
    val jwtSecret = jwtConfig.property("secret").getString()  // Read the secret
    val jwtAudience = jwtConfig.property("audience").getString()  // Read the audience
    val jwtDomain = jwtConfig.property("issuer").getString()  // Read the domain
    val jwtRealm = jwtConfig.property("realm").getString()  // Read the realm

    // Log the JWT configuration values
    logger.info("JWT Secret: $jwtSecret")
    logger.info("JWT Audience: $jwtAudience")
    logger.info("JWT Issuer: $jwtDomain")
    logger.info("JWT Realm: $jwtRealm")

    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                return@validate if (credential.payload.audience.contains(jwtAudience)) {
                    // Log the JWT token details (for debugging purposes)
                    logger.info("Successfully validated token with subject: ${credential.payload.subject}")
                    JWTPrincipal(credential.payload)
                } else {
                    // Log error if the audience does not match
                    logger.error("Token validation failed: Audience mismatch for token: ${credential.payload}")
                    null
                }
            }
        }
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            proceed() // Proceed with normal request handling
        } catch (e: AuthenticationException) {
            // Log the failed authentication attempt
            logger.error("Authentication failed: ${e.localizedMessage}", e)
            call.respondText("Invalid or expired token", status = HttpStatusCode.Unauthorized)
        }
    }
}

// Helper function to extract and log the JWT token details
fun ApplicationCall.headerData(logger: org.slf4j.Logger) {
    val token = request.headers["Authorization"]?.removePrefix("Bearer ")

    if (token != null) {
        try {
            // Decode the JWT and log the encoded parts (header + payload)
            val decodedJWT = JWT.decode(token)
            logger.info("JWT Header: ${decodedJWT.header}")
            logger.info("JWT Payload: ${decodedJWT.payload}")
        } catch (e: JWTDecodeException) {
            logger.error("Failed to decode JWT: ${e.localizedMessage}", e)
        }
    } else {
        logger.warn("No JWT token found in Authorization header")
    }
}
