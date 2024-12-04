package com.example.service.cache

import com.example.models.CacheCharacters
import com.example.models.CacheEntry
import com.example.models.CharacterDBData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class CacheService {

    private val json = Json { ignoreUnknownKeys = true }
    private val logger: Logger = LoggerFactory.getLogger(CacheService::class.java)

    /**
     * Check if the same query is cached for a user.
     * @param characterId The character's UUID to check in the cache.
     * @return CacheEntry if the query is cached and valid, or null if not.
     */
    fun checkUserCache(characterId: UUID): CacheEntry? {
        logger.info("Checking cache for character ID: $characterId")
        return transaction {
            val cacheEntry = CacheCharacters
                .selectAll()
                .where { CacheCharacters.characterId eq characterId }
                .orderBy(CacheCharacters.updatedAt, SortOrder.DESC)
                .limit(1)
                .singleOrNull()

            if (cacheEntry != null) {
                val expiresAt = cacheEntry[CacheCharacters.expiresAt]
                if (expiresAt.isAfter(Instant.now())) {
                    logger.info("Cache hit for character ID: $characterId, expires at: $expiresAt")
                    toCacheEntry(cacheEntry)
                } else {
                    logger.warn("Cache expired for character ID: $characterId, expired at: $expiresAt")
                    null // Cache expired
                }
            } else {
                logger.info("Cache miss for character ID: $characterId")
                null // Cache not found
            }
        }
    }

    /**
     * Cache a character's data in the cache table.
     */
    fun cacheCharacterData(characterId: UUID, characterData: CharacterDBData) {
        logger.info("Caching data for character ID: $characterId")
        val dataJson = json.encodeToString(characterData)

        val expiresAt = Instant.now().plusSeconds(24 * 60 * 60) // 24 hours from now
        logger.debug("Calculated expiration time for character ID $characterId: $expiresAt")

        transaction {
            // Check if the character is already in the cache
            val existingEntry = CacheCharacters
                .selectAll()
                .where { CacheCharacters.characterId eq characterId }
                .limit(1)
                .singleOrNull()

            if (existingEntry == null) {
                logger.info("No existing cache entry for character ID: $characterId. Inserting new entry.")
                CacheCharacters.insert {
                    it[this.characterId] = characterId
                    it[this.data] = dataJson
                    it[this.createdAt] = Instant.now()
                    it[this.updatedAt] = Instant.now()
                    it[this.expiresAt] = expiresAt
                }
                logger.debug("Inserted cache entry for character ID: $characterId")
            } else {
                logger.info("Existing cache entry found for character ID: $characterId. Updating entry.")
                CacheCharacters.update({ CacheCharacters.characterId eq characterId }) {
                    it[this.data] = dataJson
                    it[this.updatedAt] = Instant.now()
                    it[this.expiresAt] = expiresAt
                }
                logger.debug("Updated cache entry for character ID: $characterId")
            }
        }
    }

    /**
     * Helper function to map a ResultRow to a CacheEntry data class.
     */
    private fun toCacheEntry(row: ResultRow): CacheEntry {
        logger.debug("Mapping database row to CacheEntry object for character ID: ${row[CacheCharacters.characterId]}")
        return CacheEntry(
            id = row[CacheCharacters.id],
            characterId = row[CacheCharacters.characterId],
            data = row[CacheCharacters.data]
        ).also {
            logger.debug("Mapped CacheEntry: $it")
        }
    }
}
