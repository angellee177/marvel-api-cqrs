package com.example.utils

import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Convert Marvel API timestamp string to Instant
fun String.toInstant(): Instant {
    val dateFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"), // Full timestamp with timezone
        DateTimeFormatter.ofPattern("yyyy-MM-dd") // Date-only format
    )

    for (formatter in dateFormats) {
        try {
            val offsetDateTime = OffsetDateTime.parse(this, formatter)
            return offsetDateTime.toInstant()
        } catch (e: DateTimeParseException) {
            // If the parsing fails for this format, try the next one
            continue
        }
    }

    // If none of the formats work, throw an exception
    throw DateTimeParseException("Invalid date format: $this", this, 0)
}

// Convert Instant to gRPC Timestamp
fun Instant.toTimestamp(): Timestamp = Timestamp.newBuilder()
    .setSeconds(epochSecond)
    .setNanos(nano)
    .build()

// Convert String directly to gRPC Timestamp
fun String.toGrpcTimestamp(): Timestamp = this.toInstant().toTimestamp()



