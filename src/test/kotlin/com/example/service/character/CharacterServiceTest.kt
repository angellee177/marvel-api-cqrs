package com.example.service.character

import com.example.DatabaseFactory
import com.example.models.CharacterDBData
import com.example.models.Characters
import com.example.service.cache.CacheService
import com.example.thirdparty.MarvelApiClient
import com.example.thirdparty.MarvelCharacter
import com.example.thirdparty.MarvelData
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class CharacterServiceTest {

    private lateinit var characterService: CharacterService
    private val cacheService: CacheService = Mockito.mock(CacheService::class.java)
    private val marvelApiClient: MarvelApiClient = Mockito.mock(MarvelApiClient::class.java)

    private val mockCharacters = listOf(
        MarvelCharacter(
            1,
            "John",
            "Description",
            "2024-09-24T11:11:31-0400"
        )
    )

    @BeforeEach
    fun setup() {
        val appConfig = HoconApplicationConfig(ConfigFactory.load("application.conf"))
        val dbUrl = appConfig.property("db.jdbcUrl").getString()
        val dbUser = appConfig.property("db.dbUser").getString()
        val dbPassword = appConfig.property("db.dbPassword").getString()
        val driverClassName = appConfig.property("db.driverClassName").getString()

        System.setProperty("db.jdbcUrl", dbUrl)
        System.setProperty("db.dbUser", dbUser)
        System.setProperty("db.dbPassword", dbPassword)
        System.setProperty("db.driverClassName", driverClassName)

        DatabaseFactory.init(testMigrationPath = "classpath:db/migration_test")

        characterService = CharacterService(cacheService, marvelApiClient)
    }

    @AfterEach
    fun tearDown() {
        transaction {
            Characters.deleteAll()
        }
    }

    @Test
    fun `test fetchCharacters returns cached data`() = runBlocking {
        val queryParams = mapOf("name" to "John")
        val cachedData = listOf(
            CharacterDBData("1", "John", "Cached description", "2024-09-24T11:11:31-0400")
        )

        val cacheKey = cacheService.generateCacheKey(queryParams)

        Mockito.`when`(cacheService.fetchMatchingCacheEntries(
            cacheKey,
            5,
            0,
        )).thenReturn(cachedData)

        val result = characterService.fetchCharacters(queryParams)

        assertEquals(1, result.size)
        assertEquals("John", result[0].name)
    }

    @Test
    fun `test fetchCharacters fetches from API on cache miss`() = runBlocking {
        val queryParams = mapOf("name" to "John")
        val apiResponse = MarvelData(
            count = 1,
            results = mockCharacters,
            limit = 1,
        )

        val cacheKey = cacheService.generateCacheKey(queryParams)

        Mockito.`when`(cacheService.fetchMatchingCacheEntries(
            cacheKey,
            1,
            0,
        )).thenReturn(emptyList())
        Mockito.`when`(marvelApiClient.fetchCharacters(queryParams)).thenReturn(apiResponse)

        val result = characterService.fetchCharacters(queryParams)

        assertEquals(1, result.size)
        assertEquals("John", result[0].name)
    }

    @Test
    fun `test fetchCharacters saves API data to database`() = runBlocking {
        val queryParams = mapOf("name" to "John")
        val apiResponse = MarvelData(
            count = 1,
            results = mockCharacters,
            limit = 1,
        )

        val cacheKey = cacheService.generateCacheKey(queryParams)

        Mockito.`when`(cacheService.fetchMatchingCacheEntries(
            cacheKey,
            1,
            0,
        )).thenReturn(emptyList())
        Mockito.`when`(marvelApiClient.fetchCharacters(queryParams)).thenReturn(apiResponse)

        characterService.fetchCharacters(queryParams)

        transaction {
            val dbCharacters = Characters.selectAll().toList()
            assertEquals(1, dbCharacters.size)
            assertEquals("John", dbCharacters[0][Characters.name])
        }
    }

    @Test
    fun `test fetchCharacters caches fetched data`() = runBlocking {
        val queryParams = mapOf("name" to "John")

        val cacheKey = cacheService.generateCacheKey(queryParams)

        val apiResponse = MarvelData(
            count = 1,
            results = mockCharacters,
            limit = 1
        )
        Mockito.`when`(cacheService.generateCacheKey(queryParams)).thenReturn("name=John")

        Mockito.`when`(cacheService.fetchMatchingCacheEntries(
            cacheKey,
            1,
            0,
        )).thenReturn(emptyList())
        Mockito.`when`(marvelApiClient.fetchCharacters(queryParams)).thenReturn(apiResponse)

        characterService.fetchCharacters(queryParams)

        Mockito.verify(cacheService).cacheCharacterData(Mockito.anyString(), Mockito.anyList())
    }
}
