package com.example.grpc

import characters.CharacterServiceGrpc
import characters.Characters.CharacterList
import characters.Characters.Character
import characters.Characters.FetchCharactersRequest
import com.example.models.CharacterDBData
import com.example.service.character.CharacterService
import com.example.utils.toInstant
import io.grpc.Status
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.format.DateTimeParseException

class CharacterServiceImpl(
    private val characterService: CharacterService,
) : CharacterServiceGrpc.CharacterServiceImplBase() {
    private val logger = LoggerFactory.getLogger(CharacterServiceImpl::class.java)

    override fun fetchCharacters(
        request: FetchCharactersRequest,
        responseObserver: StreamObserver<CharacterList>
    ) {
        val limit = if (request.limit == 0) 5 else request.limit // Default to 5 if unset or 0
        val offset = if (request.offset == 0) 0 else request.offset
        val modifiedSince = request.modifiedSince

        if (modifiedSince.isNotBlank() && !validateModifiedInputType(modifiedSince)) {
            val errorMessage = "Invalid 'modifiedSince' input: '$modifiedSince' does not meet the required format or criteria."
            logger.error(errorMessage)
            val e = Status.INVALID_ARGUMENT.withDescription(errorMessage).asRuntimeException()
            responseObserver.onError(e)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert FetchCharactersRequest to query parameters map
                val queryParams = mutableMapOf<String, String>()
                request.name?.let { queryParams["name"] = it }
                request.nameStartsWith?.let { queryParams["nameStartsWith"] = it }
                queryParams["modifiedSince"] = modifiedSince
                queryParams["offset"] = offset.toString()
                queryParams["limit"] = limit.toString()

                logger.info("Received fetch characters request with parameters: $queryParams")

                // Call the CharacterService to fetch the characters
                val characterList: List<CharacterDBData> = characterService.fetchCharacters(queryParams)

                // Map the fetched CharacterDBData to Protobuf Character response format
                val charactersResponse = CharacterList.newBuilder()
                    .addAllCharacters(
                        characterList.map { dbData ->
                            Character.newBuilder()
                                .setMarvelId(dbData.marvelId)
                                .setName(dbData.name)
                                .setDescription(dbData.description)
                                .setLastModified(dbData.lastModified)
                                .build()
                        }
                    )
                    .build()

                responseObserver.onNext(charactersResponse)
                responseObserver.onCompleted()
                logger.info("Successfully responded with character list")
            } catch (e: Exception) {
                logger.error("Error fetching characters: ${e.localizedMessage}", e)
                responseObserver.onError(e)
            }
        }
    }

    private fun validateModifiedInputType(
        modifiedSince: String,
    ): Boolean {
        // Validate the 'modifiedSince' field if it's not empty
        try {
            // Validate and convert 'modifiedSince' string to Instant
            modifiedSince.toInstant()
            return true
        } catch (e: DateTimeParseException) {
            // Invalid date format
            logger.error("Invalid date format for modifiedSince: $modifiedSince, error: ${e.message}")
            return false
        }
    }
}