package com.example

import com.example.service.userProfile.ProfileService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import startGrpcServer

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module(isTest: Boolean = false) {
    // Ktor setup for serialization, security, logging, etc.
    configureSerialization()
    configureSecurity()
    configureLogging()
    DatabaseFactory.init()

    // Start the gRPC server
    startGrpcServer(ProfileService(), isTest)

    // Ktor Routing for REST (if needed)
    configureRouting()
}
