package com.example.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ColumnType
import org.postgresql.util.PGobject

// Custom JsonB Column Type for serializing and deserializing JSON data
class JsonBColumnType<T>(private val serializer: KSerializer<T>) : ColumnType<T>() {

    // Specify the SQL type for the column
    override fun sqlType(): String = "JSONB"

    // Deserialize the database value to the appropriate Kotlin type (T)
    override fun valueFromDB(value: Any): T {
        return when (value) {
            is PGobject -> Json.decodeFromString(serializer, value.value ?: "null")
            is String -> Json.decodeFromString(serializer, value)
            else -> error("Unexpected value: $value")
        }
    }
}
