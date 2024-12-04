package com.example.thirdparty

import kotlinx.serialization.Serializable

@Serializable
data class MarvelApiResponse(
    val data: MarvelData
)

@Serializable
data class MarvelData(
    val limit: Int,
    val total: Int,
    val results: List<MarvelCharacter>
)

@Serializable
data class MarvelCharacter(
    val id: Int,
    val name: String,
    val description: String?,
    val modified: String // ISO-8601 timestamp
)
