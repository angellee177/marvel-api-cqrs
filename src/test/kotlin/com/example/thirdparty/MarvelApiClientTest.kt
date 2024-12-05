package com.example.thirdparty

import io.kotest.matchers.be
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import io.kotest.matchers.*
import io.kotest.matchers.types.shouldBeTypeOf

class MarvelApiClientTest {
    @Test
    @Tag("adapter")
    fun `can fetch data from marvel APIs`() = runTest {
        val apiResponse = MarvelApiClient().fetchCharacters()

        apiResponse.results.first().shouldBeTypeOf<MarvelCharacter>()
    }

    @Test
    @Tag("adapter")
    fun `can fetch data with query`() = runTest {
        val queryParams = mutableMapOf<String, String>()
        queryParams["name"] = "Hulk"

        val apiResponse = MarvelApiClient().fetchCharacters(queryParams)

        apiResponse.total should be(1)
        apiResponse.results.first().name shouldBe "Hulk"
    }
}