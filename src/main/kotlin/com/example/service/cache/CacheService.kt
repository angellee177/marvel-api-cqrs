package com.example.service.cache

import com.example.lib.JsonConfig
import com.example.models.CacheCharacters
import com.example.models.CharacterDBData
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CacheService {

    private val logger: Logger = LoggerFactory.getLogger(CacheService::class.java)

    /**
     * Generate a unique cache key from query parameters.
     */
    fun generateCacheKey(params: Map<String, String?>): String {
        return params.entries
            .filter { !it.value.isNullOrEmpty() } // Exclude null or empty parameters
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
    }

    /**
     * Fetch all matching cache entries based on query parameters.
     * @param queryParams Map of query parameters to filter cache entries.
     * @return List of matching CacheEntry objects.
     */
    fun fetchMatchingCacheEntries(cacheKey: String, limit: Int, offset: Int): List<CharacterDBData> {
        logger.info("Fetching matching cache entries for query parameters: $cacheKey")

        // Build SQL conditions
        logger.info("Successfully built conditions: $cacheKey")

        return try {
            // Execute the query inside a transaction
            transaction {
                CacheCharacters
                    .select(CacheCharacters.data) // Select only the 'data' column
                    .where {
                        (CacheCharacters.cacheKey eq cacheKey) and
                                (CacheCharacters.expiresAt greaterEq Instant.now()) // Check for expiration
                    }
                    .orderBy(CacheCharacters.updatedAt, SortOrder.DESC)
                    .limit(limit)
                    .offset(offset.toLong())
                    .map { toCacheEntry(it) }
                    .flatten()// Convert rows to CacheEntry objects
            }.also { results ->
                logger.info("Found ${results.size} matching cache entries for query.")
            }
        } catch (e: Exception) {
            logger.error("Error fetching matching cache entries: ${e.message}", e)
            throw e // Rethrow the exception
        }
    }

    /**
     * Cache list of characters data in the cache table.
     */
    fun cacheCharacterData(cacheKey: String, characters: List<CharacterDBData>) {
        logger.info("Caching data for cacheKey: $cacheKey")

        if (characters.isEmpty()) {
            return
        }

        logger.info("ListCharacters: $characters")
        val dataJson = JsonConfig.json.encodeToString(characters)

        logger.info("Successfully encodeToString: $dataJson")
        val expiresAt = Instant.now().plusSeconds(24 * 60 * 60) // 24 hours from now

        transaction {
            logger.info("Start storing cache: $dataJson, with cacheKey: $cacheKey")

            CacheCharacters.insert {
                it[this.cacheKey] = cacheKey
                it[this.data] = characters
                it[this.expiresAt] = expiresAt
            }
        }.also {
            logger.debug("Cached data for cacheKey: $cacheKey")
        }
    }

    /**
     * Helper function to map a ResultRow to a CacheEntry data class.
     */
    private fun toCacheEntry(row: ResultRow): List<CharacterDBData> = row[CacheCharacters.data]

    /**
     * Parse 'modifiedSince' value to Instant, handling full datetime, datetime without colon in timezone, and date-only formats.
     */
    fun parseModifiedSince(value: String): Instant {
        return when {
            value.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{4}""")) -> {
                // Handle datetime with timezone offset without colon (e.g., 2024-09-24T11:11:31-0400)
                val formattedValue = value.substring(0, 22) + ":" + value.substring(22) // Add colon to timezone offset
                OffsetDateTime.parse(formattedValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
            }

            value.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}""")) -> {
                // Handle standard ISO-8601 datetime with colon in timezone offset
                OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
            }

            value.matches(Regex("""\d{4}-\d{2}-\d{2}""")) -> {
                // Handle date-only format (e.g., 2023-04-05)
                LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneOffset.UTC).toInstant()
            }

            else -> throw IllegalArgumentException("Invalid format for 'modifiedSince': $value")
        }
    }
}
