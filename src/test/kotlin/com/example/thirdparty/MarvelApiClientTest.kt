package com.example.thirdparty

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class MarvelApiClientTest {
    @Test
    @Tag("adapter")
    fun `can fetch data from marvel APIs`() = runTest {
        val result = MarvelApiClient().fetchCharacters()

        println(result)
    }
}