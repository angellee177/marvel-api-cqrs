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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CharacterService(
    private val cacheService: CacheService,
    private val marvelApiClient: MarvelApiClient,
) {
    private val logger: Logger = LoggerFactory.getLogger(CharacterService::class.java)

    suspend fun fetchCharacters(
        queryParams: Map<String, String>,
    ): List<CharacterDBData> = withContext(Dispatchers.IO) {
        logger.info("Fetching Characters is starting")

        val cacheKey = cacheService.generateCacheKey(queryParams)

        try {
            // Parse limit and offset from queryParams
            val limit = queryParams["limit"]?.toIntOrNull() ?: 5
            val offset = queryParams["offset"]?.toIntOrNull() ?: 0

            // Check cache first
            logger.info("Checking from cache")
            val cachedData: List<CharacterDBData> = cacheService.fetchMatchingCacheEntries(queryParams)
            if (cachedData.isNotEmpty()) {
                logger.info("Returning cached data for query key: $cacheKey")

                // return the cache data
                return@withContext pagination(cachedData, limit, offset)
            }

            // Fetch fresh data from the API
            logger.info("Cache miss. Fetching fresh data for query key: $cacheKey")
            val freshData: MarvelData = marvelApiClient.fetchCharacters(queryParams)

            if (freshData.count == 0) {
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
    ): List<CharacterDBData> {
        val characters = mutableListOf<CharacterDBData>()

        transaction {
            // Process the results one by one
            results.forEach { result ->
                val description = result.description ?: ""
                val lastModified = result.modified.toInstant()

                // Create CharacterDBData instance
                val characterDBData = CharacterDBData(
                    marvelId = result.id.toString(),
                    name = result.name,
                    description = description,
                    lastModified = result.modified,
                )

                logger.info("Checking if character with name: ${characterDBData.name} exists.")

                // Check if the character already exists based on the unique 'name'
                val existingCharacter = Characters
                    .selectAll()
                    .where { Characters.name eq  characterDBData.name }
                    .singleOrNull()

                // If the character does not exist, insert it
                if (existingCharacter == null) {
                    logger.info("Saving new character: ${characterDBData.name} with Marvel ID: ${characterDBData.marvelId}")

                    // Insert a single character at a time
                    Characters.insert {
                        it[Characters.marvelId] = characterDBData.marvelId
                        it[Characters.name] = characterDBData.name
                        it[Characters.description] = description
                        it[Characters.lastModified] = lastModified
                    }

                    logger.info("Successfully saved new character: ${characterDBData.name} with Marvel ID: ${characterDBData.marvelId}")
                    characters.add(characterDBData)
                } else {
                    logger.info("Character with name: ${characterDBData.name} already exists, skipping.")
                }
            }
        }

        return characters
    }

    private fun pagination(
        data: List<CharacterDBData>,
        limit: Int = 5,
        offset: Int = 0,
    ): List<CharacterDBData> {
        val startIndex = offset
        val endIndex = (offset + limit).coerceAtMost(data.size)
        return if (startIndex < data.size) data.subList(startIndex, endIndex) else emptyList()
    }
}
