package com.example.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object Characters : Table() {
    val id: Column<UUID> = uuid("id").autoGenerate()
    val marvelId: Column<String> = varchar("marvelId", 100)
    val name: Column<String> = varchar("name", 100)
    val description: Column<String> = varchar("description", 100)
    val lastModified: Column<Instant> = timestamp("lastModified")
    val updatedAt: Column<Instant> = timestamp("updatedAt").default(Instant.now())
}

@Serializable
data class CharacterDBData(
    val marvelId: String,
    val name: String,
    val description: String,
    @Contextual
    val lastModified: Instant,
) {
    // Convert Character object to JSON string
    fun toJson(): String {
        return Json.encodeToString(this)
    }

    // Deserialize JSON string into a Character object
    companion object {
        fun fromJson(json: String): CharacterDBData {
            return Json.decodeFromString<CharacterDBData>(json)
        }
    }
}