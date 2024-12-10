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
        logger.info("Successfully generate cacheKey: $cacheKey")

        try {
            // Parse limit and offset from queryParams
            val limit = queryParams["limit"]?.toIntOrNull() ?: 5
            val offset = queryParams["offset"]?.toIntOrNull() ?: 0

            // Check cache first
            logger.info("Checking from cache")
            val cachedData: List<CharacterDBData> = transaction {
                cacheService.fetchMatchingCacheEntries(cacheKey, limit, offset)
            }

            logger.info("cache data are found: $cachedData")

            if (cachedData.isNotEmpty()) {
                logger.info("Returning cached data for query key: $cacheKey")
                return@withContext pagination(cachedData, limit, offset)
            }

            // Fetch fresh data from the API
            logger.info("Cache miss. Fetching fresh data for query key: $cacheKey")
            val freshData: MarvelData = marvelApiClient.fetchCharacters(queryParams)

            if (freshData.count == 0) {
                logger.info("No characters found for query key: $cacheKey")
                return@withContext emptyList()
            }

            logger.info("Successfully fetch from Marvel Api, total characters: ${freshData.results}")

            // Save characters to DB if they do not exist
            transaction {
                saveCharactersToDb(freshData.results)
            }

            // Format characters into CharacterDBData
            val formattedCharacters: List<CharacterDBData> = formatCharacters(freshData.results)

            // Cache processed data
            coroutineScope {
                launch {
                    cacheService.cacheCharacterData(cacheKey, formattedCharacters)
                }
            }

            logger.info("Successfully fetched and cached data for query key: $cacheKey")

            // return the characters
            return@withContext pagination(formattedCharacters, limit, offset)
        } catch (e: Exception) {
            logger.error("Error handling character request: ${e.message}", e)
            throw Error("Failed to fetch characters: ${e.message}")
        }
    }

    /**
     * Processes and saves characters in batches to handle large data sets efficiently.
     */
    private fun saveCharactersToDb(
        results: List<MarvelCharacter>,
    ) {
        transaction {
            // Process the results one by one
            results.forEach { result ->
                val description = result.description ?: ""
                val lastModified = result.modified.toInstant()
                logger.info("try to convert lastModified: $lastModified")

                logger.info("Checking if character with name: ${result.name} exists.")

                // Check if the character already exists based on the unique 'name'
                val existingCharacter = Characters
                    .selectAll()
                    .where { Characters.name eq  result.name }
                    .singleOrNull()

                // If the character does not exist, insert it
                if (existingCharacter == null) {
                    logger.info("Saving new character: ${result.name} with Marvel ID: ${result.id}")

                    // Insert a single character at a time
                    Characters.insert {
                        it[Characters.marvelId] = result.id.toString()
                        it[Characters.name] = result.name
                        it[Characters.description] = description
                        it[Characters.lastModified] = lastModified
                    }

                    logger.info("Successfully saved new character: ${result.name} with Marvel ID: ${result.id}")
                } else {
                    logger.info("Character with name: ${result.name} already exists, skipping.")
                }
            }
        }
    }

    /**
     * Formats MarvelCharacter instances into CharacterDBData.
     * @param results List of MarvelCharacter to be formatted.
     * @return List of CharacterDBData.
     */
    private fun formatCharacters(results: List<MarvelCharacter>): List<CharacterDBData> {
        return results.map { result ->
            val description = result.description ?: ""
            val lastModified = result.modified.toInstant()

            logger.info("Formatted character: ${result.name}, Last Modified: $lastModified")

            CharacterDBData(
                marvelId = result.id.toString(),
                name = result.name,
                description = description,
                lastModified = result.modified,
            )
        }
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
