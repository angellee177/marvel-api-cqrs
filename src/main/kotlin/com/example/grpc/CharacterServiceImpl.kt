package com.example.grpc

import characters.CharacterServiceGrpc
import characters.Characters.CharacterList
import characters.Characters.Character
import characters.Characters.FetchCharactersRequest
import com.example.models.CharacterDBData
import com.example.service.character.CharacterService
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class CharacterServiceImpl(
    private val characterService: CharacterService,
) : CharacterServiceGrpc.CharacterServiceImplBase() {
    private val logger = LoggerFactory.getLogger(CharacterServiceImpl::class.java)

    override fun fetchCharacters(
        request: FetchCharactersRequest,
        responseObserver: StreamObserver<CharacterList>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logger.info("Received fetch characters request with parameters: $request")

                // Convert FetchCharactersRequest to query parameters map
                val queryParams = mutableMapOf<String, String>()
                request.name?.let { queryParams["name"] = it }
                request.nameStartsWith?.let { queryParams["nameStartsWith"] = it }
                request.modifiedSince?.let { queryParams["modifiedSince"] = it }
                queryParams["offset"] = request.offset.toString()
                queryParams["limit"] = request.limit.toString()

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
                                .setLastModified(dbData.lastModified.toString())
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
}