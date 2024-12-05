package com.example.service.cache

import com.example.models.CacheCharacters
import com.example.models.CharacterDBData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class CacheService {

    private val json = Json { ignoreUnknownKeys = true }
    private val logger: Logger = LoggerFactory.getLogger(CacheService::class.java)

    /**
     * Generate a unique cache key from query parameters.
     */
    fun generateCacheKey(params: Map<String, String?>): String {
        return params.entries
            .filter { it.value != null }
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
    }

    /**
     * Fetch all matching cache entries based on query parameters.
     * @param queryParams Map of query parameters to filter cache entries.
     * @return List of matching CacheEntry objects.
     */
    fun fetchMatchingCacheEntries(queryParams: Map<String, String?>): List<CharacterDBData> {
        logger.info("Fetching matching cache entries for query parameters: $queryParams")
        val conditions = buildConditions(queryParams)

        return transaction {
            CacheCharacters.selectAll()
                .where { conditions and (CacheCharacters.expiresAt greaterEq Instant.now()) }
                .orderBy(CacheCharacters.updatedAt, SortOrder.DESC)
                .map { toCacheEntry(it) }
        }.also { results ->
            logger.info("Found ${results.size} matching cache entries for query.")
        }
    }

    /**
     * Build SQL conditions dynamically based on query parameters.
     */
    private fun buildConditions(queryParams: Map<String, String?>): Op<Boolean> {
        var conditions: Op<Boolean> = Op.TRUE
        queryParams.forEach { (key, value) ->
            if (!value.isNullOrEmpty()) {
                when (key) {
                    "name" -> conditions = conditions and (CacheCharacters.cacheKey like "%\"name\":\"$value\"%")
                    "nameStartsWith" -> conditions =
                        conditions and (CacheCharacters.cacheKey like "%\"name\":\"$value%")

                    "modifiedSince" -> {
                        // lastModified search query from user will be Date type
                        val modifiedSinceInstant = Instant.parse(value)

                        // Check `modified` field embedded in JSON stored in cacheKey column
                        conditions = conditions and (
                                CacheCharacters.data like "%\"lastModified\":\"$modifiedSinceInstant\"%")
                    }
                }
            }
        }
        return conditions
    }

    /**
     * Cache list of characters data in the cache table.
     */
    fun cacheCharacterData(cacheKey: String, characters: List<CharacterDBData>) {
        logger.info("Caching data for cacheKey: $cacheKey")
        val dataJson = json.encodeToString(characters)
        val expiresAt = Instant.now().plusSeconds(24 * 60 * 60) // 24 hours from now

        transaction {
            CacheCharacters.insert {
                it[this.data] = dataJson
                it[this.createdAt] = Instant.now()
                it[this.updatedAt] = Instant.now()
                it[this.expiresAt] = expiresAt
            }
        }.also {
            logger.debug("Cached data for cacheKey: $cacheKey")
        }
    }

    /**
     * Helper function to map a ResultRow to a CacheEntry data class.
     */
    private fun toCacheEntry(row: ResultRow): CharacterDBData =
        CharacterDBData.fromJson(row[CacheCharacters.data])
}
