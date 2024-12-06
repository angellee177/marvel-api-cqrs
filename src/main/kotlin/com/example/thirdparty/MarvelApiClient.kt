package com.example.thirdparty

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.security.MessageDigest

class MarvelApiClient {

    private val logger = LoggerFactory.getLogger(MarvelApiClient::class.java)

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
        expectSuccess = false
    }

    private val privateApiKey: String
    private val apiKey: String

    init {
        val marvelConfig = HoconApplicationConfig(ConfigFactory.load())
        apiKey = marvelConfig.property("marvelApi.publicApiKey").getString()
        privateApiKey = marvelConfig.property("marvelApi.privateApiKey").getString()
    }

    companion object {
        private const val baseUrl = "https://gateway.marvel.com:443/v1/public/characters"
    }

    /**
     * Fetch characters from Marvel API based on query parameters.
     *
     * @param queryParams Optional map of query parameters to filter characters
     * @return MarvelData object or null if the request fails
     */
    suspend fun fetchCharacters(queryParams: Map<String, String>? = null): MarvelData {
        return withContext(Dispatchers.IO) {
            retry(times = 3, initialDelay = 1_000) {
                try {
                    val ts = System.currentTimeMillis().toString()
                    val hash = generateHash(ts, privateApiKey, apiKey)

                    // If queryParams is null, start with an empty map
                    val filteredQueryParams = queryParams?.filter { it.value?.isNotEmpty() == true } ?: emptyMap()

                    // Add default limit and offset if not already present in queryParams
                    val defaultParams = mapOf("limit" to "5", "offset" to "0")
                    val finalQueryParams = filteredQueryParams + defaultParams.filter { it.key !in filteredQueryParams }

                    val url = buildUrl(finalQueryParams, ts, hash)
                    val response = client.get(url)

                    logger.debug("Fetching characters from Marvel API. URL: $url")

                    if (response.status == HttpStatusCode.OK) {
                        val rawResponse = response.bodyAsText()

                        val json = Json { ignoreUnknownKeys = true }
                        val apiResponse: MarvelApiResponse = json.decodeFromString(rawResponse)
                        logger.debug("Response received and successfully deserialized. apiResponse: $apiResponse")

                        // Pass the deserialized object to transformToMarvelData
                        return@retry transformToMarvelData(apiResponse)
                    } else {
                        val errorBody = response.bodyAsText()
                        logger.error("Failed to fetch characters. Status: ${response.status}, Error: $errorBody")
                        emptyMarvelData
                    }
                } catch (e: Exception) {
                    logger.error("Error fetching data from Marvel API: ${e.message}", e)
                    emptyMarvelData
                }
            }
        }
    }

    /**
     * Transform MarvelApiResponse into a MarvelData object.
     *
     * @param apiResponse Deserialized MarvelApiResponse object
     * @return MarvelData object or null
     */
    private fun transformToMarvelData(apiResponse: MarvelApiResponse): MarvelData {
        val data = apiResponse.data

        if (data.count == 0 && data.results.isEmpty()) {
            logger.warn("No characters found in the API response.")
            return emptyMarvelData
        }

        return MarvelData(
            limit = data.limit ?: 0,
            count = data.count ?: 0,
            results = data.results.map { marvelCharacter ->
                MarvelCharacter(
                    id = marvelCharacter.id,
                    name = marvelCharacter.name,
                    description = marvelCharacter.description ?: "No description available",
                    modified = marvelCharacter.modified
                )
            }
        )
    }

    /**
     * Generate an MD5 hash for Marvel API authentication.
     */
    private fun generateHash(ts: String, privateApiKey: String, publicApiKey: String): String {
        val input = ts + privateApiKey + publicApiKey
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Build the request URL dynamically with query parameters and authentication.
     */
    private fun buildUrl(queryParams: Map<String, String>, ts: String, hash: String): String {
        val urlBuilder = StringBuilder(baseUrl)
        urlBuilder.append("?apikey=$apiKey&ts=$ts&hash=$hash")

        queryParams.forEach { (key, value) ->
            urlBuilder.append("&$key=$value")
        }

        return urlBuilder.toString()
    }

    /**
     * Helper function for retrying operations.
     */
    private suspend fun <T> retry(times: Int, initialDelay: Long, block: suspend () -> T): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                logger.warn("Retrying after failure: ${e.message}")
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return block()
    }

    val emptyMarvelData = MarvelData(
        limit = 0,
        count = 0,
        results = emptyList(),
    )
}