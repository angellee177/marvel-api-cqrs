package com.example.utils

import io.grpc.Status
import org.slf4j.Logger

/**
 * Verifies a token and retrieves the associated email.
 *
 * @param token The JWT token to validate.
 * @param logger An instance of a logger for custom logging.
 * @return The email extracted from the token if valid.
 * @throws StatusRuntimeException if the token is missing or invalid.
 */
fun verifyToken(token: String, logger: Logger): String {
    if (token.isEmpty()) {
        logger.error("Missing token.")
        throw Status.UNAUTHENTICATED
            .withDescription("Missing token")
            .asRuntimeException()
    }

    val email = JWT.verifyTokenAndGetEmail(token)
    if (email == null) {
        logger.error("Invalid token, email is null")
        throw Status.UNAUTHENTICATED
            .withDescription("Invalid token")
            .asRuntimeException()
    }

    return email
}