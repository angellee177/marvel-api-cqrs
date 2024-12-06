package com.example.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object Characters : Table("public.characters") {
    val id: Column<UUID> = uuid("id").autoGenerate()
    val marvelId: Column<String> = varchar("marvelid", 1000)
    val name: Column<String> = varchar("name", 500)
    val description: Column<String> = text("description")
    val lastModified: Column<Instant> = timestamp("lastmodified")
    val updatedAt: Column<Instant> = timestamp("updatedat").default(Instant.now())
}

@Serializable
data class CharacterDBData(
    val marvelId: String,
    val name: String,
    val description: String,
    val lastModified: String,
)