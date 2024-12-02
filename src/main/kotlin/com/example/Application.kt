package com.example

import com.example.controller.AuthController
import com.example.service.userProfile.ProfileService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureLogging()
    DatabaseFactory.init()

    // Ktor Routing for REST (if needed)
    configureRouting()
}


