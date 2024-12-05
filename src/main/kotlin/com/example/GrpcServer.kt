import com.example.grpc.AuthServiceImpl
import com.example.grpc.CharacterServiceImpl
import com.example.grpc.UserServiceImpl
import com.example.service.auth.AuthService
import com.example.service.character.CharacterService
import com.example.service.userProfile.ProfileService
import io.grpc.ServerBuilder
import kotlinx.io.IOException
import org.slf4j.LoggerFactory

fun startGrpcServer(
    authService: AuthService,
    profileService: ProfileService,
    characterService: CharacterService,
    isTest: Boolean = false
) {
    val logger = LoggerFactory.getLogger("GrpcServerLogger")

    if (isTest) {
        return // Skip starting the gRPC server during tests
    }

    val server = ServerBuilder.forPort(50052)
        .addService(
            AuthServiceImpl(authService)
        )
        .addService(UserServiceImpl(profileService))
        .addService(
            CharacterServiceImpl(characterService)
        )
        .build()

    try {
        // Start gRPC server in a separate thread to not block Ktor's main thread
        Thread {
            try {
                server.start()
                logger.info("gRPC server started on port 50052")

                // Block the thread to keep the gRPC server running
                server.awaitTermination()
            } catch (e: IOException) {
                logger.error("Failed to start gRPC server: ${e.message}", e)
            }
        }.start()

    } catch (e: IOException) {
        logger.error("Failed to initialize gRPC server: ${e.message}", e)
        throw e
    }
}
