package com.example.utils

object DatabaseType {
    fun getCurrentDatabase(): DatabaseType {
        // Check if it's PostgreSQL or H2 based on the environment or JDBC connection string
        return if (System.getProperty("database.type") == "postgres") {
            DatabaseType.POSTGRES
        } else {
            DatabaseType.H2
        }
    }

    enum class DatabaseType {
        POSTGRES,
        H2
    }
}
