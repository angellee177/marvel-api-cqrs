package com.example.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

object Characters : Table() {
    val id: Column<UUID> = uuid("id").autoGenerate()
    val marvelId: Column<String> = varchar("marvelId", 100)
    val name: Column<String> = varchar("name", 100)
    val description: Column<String> = varchar("description", 100)
    val lastModified: Column<Instant> = timestamp("lastModified")
    val updatedAt: Column<Instant> = timestamp("updatedAt").default(Instant.now())
}

@Serializable
data class CharacterData(
    val id: UUID,
    val marveId: String,
    val name: String,
    val description: String,
    val lastModified: Instant,
)