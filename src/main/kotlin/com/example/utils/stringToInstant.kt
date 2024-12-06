package com.example.utils

import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Convert Marvel API timestamp string (with time and timezone) to Instant
fun String.toInstantWithTime(): Instant {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ") // Handle the timezone offset
    val offsetDateTime = OffsetDateTime.parse(this, formatter) // Parse the string into OffsetDateTime
    return offsetDateTime.toInstant() // Convert OffsetDateTime to Instant
}

// Convert date-only string (without time) to Instant
fun String.toInstantWithDateOnly(): Instant {
    // Add a default time of "00:00:00" and a timezone offset of "+00:00"
    val formattedDate = "${this}T00:00:00+00:00"
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX") // Date-only format with time and timezone
    val offsetDateTime = OffsetDateTime.parse(formattedDate, formatter) // Parse the string into OffsetDateTime
    return offsetDateTime.toInstant() // Convert OffsetDateTime to Instant
}

// Convert String to Instant with format checks
fun String.toInstant(): Instant {
    return when {
        this.matches(Regex("""\d{4}-\d{2}-\d{2}""")) -> { // Match date-only format (yyyy-MM-dd)
            this.toInstantWithDateOnly()
        }
        this.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:?\d{2}""")) -> { // Match full datetime format with timezone (yyyy-MM-dd'T'HH:mm:ss[Z|ZZZ])
            this.toInstantWithTime()
        }
        else -> throw DateTimeParseException("Invalid format for modifiedSince", this, 0)
    }
}


// Convert Instant to gRPC Timestamp
fun Instant.toTimestamp(): Timestamp = Timestamp.newBuilder()
    .setSeconds(epochSecond)
    .setNanos(nano)
    .build()

// Convert String directly to gRPC Timestamp
fun String.toGrpcTimestamp(): Timestamp = this.toInstant().toTimestamp()

