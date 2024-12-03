package com.example

import com.example.grpc.UserServiceImpl
import com.example.service.userProfile.ProfileService
import io.grpc.ServerBuilder
import kotlinx.io.IOException
import org.slf4j.LoggerFactory

fun startGrpcServer(profileService: ProfileService, isTest: Boolean = false) {
    if (isTest) {
        return // Skip starting the gRPC server during tests
    }

    val logger = LoggerFactory.getLogger("GrpcServerLogger")

    val server = ServerBuilder.forPort(50052)
        .addService(UserServiceImpl(profileService))
        .build()

    try {
        server.start()
        logger.info("gRPC server started on port 50052")

        // Use a shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down gRPC server...")
            server.shutdown() // Gracefully shutdown gRPC server
        })

        server.awaitTermination() // Block the thread to keep the server running
    } catch (e: IOException) {
        logger.error("Failed to start gRPC server: ${e.message}", e)
        throw e
    }
}
