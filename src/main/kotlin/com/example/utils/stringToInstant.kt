package com.example.utils

import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.format.DateTimeFormatter

// Convert Marvel API timestamp string to Instant
fun String.toInstant(): Instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(this))

// Convert Instant to gRPC Timestamp
fun Instant.toTimestamp(): Timestamp = Timestamp.newBuilder()
    .setSeconds(epochSecond)
    .setNanos(nano)
    .build()

// Convert String directly to gRPC Timestamp
fun String.toGrpcTimestamp(): Timestamp = this.toInstant().toTimestamp()

