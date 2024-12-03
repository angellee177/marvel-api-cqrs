package com.example.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object ProfileUser : Table() {
    val id: Column<UUID> = uuid("id").autoGenerate()
    val name: Column<String> = varchar("name", 100)
    val email: Column<String> = varchar("email", 100)
    val password: Column<String> = varchar("password", 100)
    val updatedAt: Column<Instant> = timestamp("updatedat").default(Instant.now())
}

@Serializable
data class ProfileType(
    val id: UUID,
    val name: String,
    val email: String,
    val password: String
)