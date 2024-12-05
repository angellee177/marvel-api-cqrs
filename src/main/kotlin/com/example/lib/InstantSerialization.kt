package com.example.lib

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import java.time.Instant

object InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("java.time.Instant")

    override fun serialize(encoder: Encoder, value: Instant) {
        // Convert Instant to ISO string format
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        // Convert string back to Instant
        return Instant.parse(decoder.decodeString())
    }
}
