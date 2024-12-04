package com.example.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

// Cache Table - stores cached character data and expiration information
object CacheCharacters : Table() {
    val id: Column<UUID> = uuid("id").autoGenerate() // Cache entry identifier
    val characterId: Column<UUID> = uuid("character_id") // Foreign key to the Characters table
    val data: Column<String> = text("data") // Store the JSON data of the character
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now()) // When the cache was created
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now()) // When the cache was last updated
    val expiresAt: Column<Instant> = timestamp("expires_at") // Expiration time of the cache entry
}

// Cache data class that represents the data stored in the CacheTable
@Serializable
data class CacheEntry(
    @Contextual // Custom UUID serializer if needed
    val id: UUID,

    // Foreign key to the character's ID
    @Contextual
    val characterId: UUID,

    // The actual cached data (typically a JSON representation)
    val data: String,
)