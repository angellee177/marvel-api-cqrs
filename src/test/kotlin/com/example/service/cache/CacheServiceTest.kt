package com.example.service.cache

import com.example.DatabaseFactory
import com.example.models.CacheCharacters
import com.example.models.CharacterDBData
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CacheServiceTest {
    val characters = listOf(
        CharacterDBData(
            "1",
            "John",
            "characters description",
            "2024-09-24T11:11:31-0400"
        )
    )

    private val cacheService: CacheService = CacheService()

    @BeforeEach
    fun setup() {
        // Load the configuration from application.conf for testing
        val appConfig = HoconApplicationConfig(ConfigFactory.load("application.conf"))

        // Read the database configuration from the application.conf
        val dbUrl = appConfig.property("db.jdbcUrl").getString()
        val dbUser = appConfig.property("db.dbUser").getString()
        val dbPassword = appConfig.property("db.dbPassword").getString()
        val driverClassName = appConfig.property("db.driverClassName").getString()

        // Set up the properties directly for the test
        System.setProperty("db.jdbcUrl", dbUrl)
        System.setProperty("db.dbUser", dbUser)
        System.setProperty("db.dbPassword", dbPassword)
        System.setProperty("db.driverClassName", driverClassName)

        // Initialize the database with test migrations
        DatabaseFactory.init(testMigrationPath = "classpath:db/migration_test")
    }

    @AfterEach
    fun tearDown() {
        transaction {
            // Clear all data from the relevant tables
            CacheCharacters.deleteAll()
        }
    }

    @Test
    fun `test generateCacheKey with valid parameters`() {
        val params = mapOf(
            "name" to "John",
            "age" to "30",
            "limit" to "5",
            "offset" to "10"
        )

        val cacheKey = cacheService.generateCacheKey(params)

        val expectedKey = "age=30&limit=5&name=John&offset=10"
        assertEquals(expectedKey, cacheKey)
    }

    @Test
    fun `test generateCacheKey with missing or empty values`() {
        val params = mapOf(
            "name" to "John",
            "age" to null,
            "limit" to "",
            "offset" to "10"
        )

        val cacheKey = cacheService.generateCacheKey(params)

        val expectedKey = "name=John&offset=10"
        assertEquals(expectedKey, cacheKey)
    }

    @Test
    fun `test fetchMatchingCacheEntries with valid query parameters`() {
        val queryParams = mapOf(
            "name" to "John",
            "limit" to "1"
        )
        val cacheKey = cacheService.generateCacheKey(queryParams)

        transaction {
            // Insert data directly into CacheCharacters table
            CacheCharacters.insert {
                it[CacheCharacters.cacheKey] = cacheKey
                it[data] = characters  // Insert the serialized data
                it[expiresAt] = Instant.now().plusSeconds(3600)
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            Thread.sleep(100)

            val result = cacheService.fetchMatchingCacheEntries(queryParams)

            println(result)
            assertEquals(1, result.size)
            assertEquals("John", result[0].name)
        }
    }

    @Test
    fun `test fetchMatchingCacheEntries with empty result`() {
        val queryParams = mapOf(
            "name" to "NonExistingName",
            "limit" to "1"
        )

        transaction {
            val result = cacheService.fetchMatchingCacheEntries(queryParams)

            assertEquals(0, result.size)
        }
    }

    //    @Test
    fun `test cacheCharacterData stores data correctly`() {
        val cacheKey = "uniqueCacheKey"

        cacheService.cacheCharacterData(cacheKey, characters)

        // Check if data is inserted into the database
//        transaction {
//            val storedData = CacheCharacters
//                .select { CacheCharacters.cacheKey eq cacheKey }
//                .map { it[CacheCharacters.data] }
//                .singleOrNull()
//
//            assertNotNull(storedData)
//            val storedCharacters = Json.decodeFromString<List<CharacterDBData>>(storedData)
//            assertEquals(characters, storedCharacters)
//        }
    }

    @Test
    fun `test fetchMatchingCacheEntries with modifiedSince query`() {
        val queryParams = mapOf(
            "name" to "John",
            "modifiedSince" to "2024-01-01"
        )
        val cacheKey = cacheService.generateCacheKey(queryParams)

        transaction {
            cacheService.cacheCharacterData(cacheKey, characters)

            val result = cacheService.fetchMatchingCacheEntries(queryParams)

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `test parseModifiedSince with valid date format`() {
        val validDate = "2024-12-01"
        val instant = cacheService.parseModifiedSince(validDate)

        assertNotNull(instant)
        assertEquals("2024-12-01T00:00:00Z", instant.toString())
    }

    @Test
    fun `test fetchMatchingCacheEntries throws exception on invalid query`() {
        val queryParams = mapOf("modifiedSince" to "invalid-date")

        assertThrows<IllegalStateException> {
            cacheService.fetchMatchingCacheEntries(queryParams)
        }
    }
}
