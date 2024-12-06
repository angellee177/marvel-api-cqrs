package com.example.models

import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID
import java.time.Instant

// Cache Table - stores cached character data and expiration information
object CacheCharacters : Table() {
    val id: Column<UUID> = uuid("\"id\"").autoGenerate() // Cache entry identifier
    val cacheKey: Column<String> = text("\"cachekey\"") // Foreign key to the Characters table
    // Corrected: Provide the correct serializer for the List<CharacterDBData>
    val data: Column<List<CharacterDBData>> = registerColumn(
        "\"data\"",
        JsonBColumnType(ListSerializer(CharacterDBData.serializer()))
    )
    val expiresAt: Column<Instant> = timestamp("\"expiresat\"") // Expiration time of the cache entry
    val createdAt: Column<Instant> = timestamp("\"createdat\"").default(Instant.now()) // When the cache was created
    val updatedAt: Column<Instant> = timestamp("\"updatedat\"").default(Instant.now()) // When the cache was last updated
}
