package com.example.lib

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant

object JsonConfig {
    // Create a Json instance with the custom Instant serializer registered
    val json = Json {
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer) // Register custom Instant serializer
        }
    }
}
