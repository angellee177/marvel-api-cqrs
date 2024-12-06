package com.example.models

import kotlinx.serialization.KSerializer
import org.jetbrains.exposed.sql.ColumnType
import org.postgresql.util.PGobject
import kotlinx.serialization.json.Json

// Custom JsonB Column Type for serializing and deserializing JSON data
class JsonBColumnType<T : Any>(private val serializer: KSerializer<T>) : ColumnType<T>() {

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

    override fun valueToDB(value: T?): Any? {
        if (value == null) return null
        @Suppress("UNCHECKED_CAST") // Explicitly suppress unchecked cast warning
        return try {
            PGobject().apply {
                type = "jsonb"
                this.value = Json.encodeToString(serializer, value as T)
            }
        } catch (e: ClassCastException) {
            throw IllegalArgumentException("Cannot cast value $value to expected type $serializer", e)
        }
    }

    override fun nonNullValueToString(value: T): String {
        @Suppress("UNCHECKED_CAST")
        return try {
            Json.encodeToString(serializer, value as T)
        } catch (e: ClassCastException) {
            throw IllegalArgumentException("Cannot serialize value $value to expected type $serializer", e)
        }
    }
}
