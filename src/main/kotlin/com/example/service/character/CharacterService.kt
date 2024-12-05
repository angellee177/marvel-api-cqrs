package com.example.service.character

import com.example.models.CharacterDBData
import com.example.models.Characters
import com.example.service.cache.CacheService
import com.example.thirdparty.MarvelApiClient
import com.example.thirdparty.MarvelCharacter
import com.example.thirdparty.MarvelData
import com.example.utils.toInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.batchInsert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class CharacterService(
    private val cacheService: CacheService,
    private val marvelApiClient: MarvelApiClient,
) {
    private val logger: Logger = LoggerFactory.getLogger(CharacterService::class.java)

    suspend fun fetchCharacters(
        queryParams: Map<String, String>,
    ): List<CharacterDBData> = withContext(Dispatchers.IO) {
        val cacheKey = cacheService.generateCacheKey(queryParams)

        try {
            // Parse limit and offset from queryParams
            val limit = queryParams["limit"]?.toIntOrNull() ?: 5
            val offset = queryParams["offset"]?.toIntOrNull() ?: 0

            // Check cache first
            val cachedData: List<CharacterDBData> = cacheService.fetchMatchingCacheEntries(queryParams)
            if (!cachedData.isNullOrEmpty()) {
                logger.info("Returning cached data for query key: $cacheKey")

                // return the cache data
                return@withContext pagination(cachedData, limit, offset)
            }

            // Fetch fresh data from the API
            logger.info("Cache miss. Fetching fresh data for query key: $cacheKey")
            val freshData: MarvelData = marvelApiClient.fetchCharacters(queryParams)

            if (freshData.total == 0) {
                logger.info("No characters found for query key: $cacheKey")
                return@withContext emptyList()
            }

            // Process and save characters in batches
            val characters: List<CharacterDBData> = processAndSaveCharactersInBatches(freshData.results)

            // Cache processed data
            coroutineScope {
                launch {
                    cacheService.cacheCharacterData(cacheKey, characters)
                }
            }

            cacheService.cacheCharacterData(cacheKey, characters)

            logger.info("Successfully fetched and cached data for query key: $cacheKey")

            // return the characters
            return@withContext pagination(characters, limit, offset)
        } catch (e: Exception) {
            logger.error("Error handling character request: ${e.message}", e)
            throw Error("Failed to fetch characters: ${e.message}")
        }
    }

    /**
     * Processes and saves characters in batches to handle large data sets efficiently.
     */
    private fun processAndSaveCharactersInBatches(
        results: List<MarvelCharacter>,
        batchSize: Int = 100
    ): List<CharacterDBData> {
        val characters = mutableListOf<CharacterDBData>()

        results.chunked(batchSize).forEach { batch ->
            // Insert characters in batch and map them to CharacterDBData
            val batchCharacters = batch.map { result ->
                val description = result.description ?: ""
                val lastModified = result.modified.toInstant()

                CharacterDBData(
                    marvelId = result.id.toString(),
                    name = result.name,
                    description = description,
                    lastModified = lastModified,
                ).also {
                    insertNewCharacter(
                        marvelId = it.marvelId,
                        name = it.name,
                        description = it.description,
                        lastModified = lastModified
                    )
                }
            }
            characters.addAll(batchCharacters)
        }

        return characters
    }

    private fun pagination(
        data: List<CharacterDBData>,
        limit: Int = 5,
        offset: Int = 0,
    ): List<CharacterDBData> {
        // Calculate the start and end indices for pagination
        val startIndex = offset
        val endIndex = (offset + limit).coerceAtMost(data.size)

        // Return a sublist based on pagination
        return if (startIndex in data.indices) {
            data.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }


    /**
     * Inserts a new character into the database.
     */
    private fun insertNewCharacter(
        marvelId: String,
        name: String,
        description: String,
        lastModified: Instant,
    ) {
        logger.info("Saving new Marvel characters in batch")
        Characters.batchInsert(listOf(mapOf(
            Characters.marvelId to marvelId,
            Characters.name to name,
            Characters.description to description,
            Characters.lastModified to lastModified
        ))) { data ->
            this[Characters.marvelId] = data[Characters.marvelId] as String
            this[Characters.name] = data[Characters.name] as String
            this[Characters.description] = data[Characters.description] as String
            this[Characters.lastModified] = data[Characters.lastModified] as Instant
        }
        logger.info("Successfully saved Marvel characters")
    }
}
