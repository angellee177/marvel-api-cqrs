package com.example.grpc

import auths.AuthServiceGrpc
import auths.Authentication.*
import com.example.models.Login
import com.example.models.ProfileType
import com.example.models.Register
import com.example.service.userProfile.ProfileService
import com.example.utils.JWT
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuthServiceImpl(
    private val profileService: ProfileService,
    private val jwt: JWT = JWT
) : AuthServiceGrpc.AuthServiceImplBase() {

    private val logger: Logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)

    override fun register(
        request: RegisterRequest,
        responseObserver: StreamObserver<RegisterResponse>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logger.info("Received gRPC Register request for email: ${request.email}")
                val register = Register(request.name, request.email, request.password)

                // Registration logic
                val hashedPassword = BCrypt.hashpw(register.password, BCrypt.gensalt())
                profileService.registerProfile(
                    register.name,
                    register.email,
                    hashedPassword
                )

                val response = RegisterResponse.newBuilder()
                    .setMessage("Registration Successful!")
                    .build()
                responseObserver.onNext(response)
                responseObserver.onCompleted()
                logger.info("gRPC Register response sent for email: ${request.email}")
            } catch (e: Exception) {
                logger.error("gRPC Register failed for email: ${request.email}", e)
                responseObserver.onError(
                    io.grpc.Status.INTERNAL
                        .withDescription("Failed to register user: ${e.localizedMessage}")
                        .asRuntimeException()
                )
            }
        }
    }

    override fun login(
        request: LoginRequest,
        responseObserver: StreamObserver<LoginResponse>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logger.info("Received gRPC Login request for email: ${request.email}")
                val login = Login(request.email, request.password)

                // Login logic
                val profile = profileService.getProfileByEmail(login.email)
                    ?: throw IllegalArgumentException("Invalid Credentials")

                if (!BCrypt.checkpw(login.password, profile.password)) {
                    logger.warn("Invalid password for email: ${login.email}")
                    throw IllegalArgumentException("Invalid Credentials")
                }

                val token = jwt.createJwtToken(profile.email)

                val response = LoginResponse.newBuilder()
                    .setToken(token)
                    .build()
                responseObserver.onNext(response)
                responseObserver.onCompleted()
                logger.info("gRPC Login response sent for email: ${request.email}")
            } catch (e: Exception) {
                logger.error("gRPC Login failed for email: ${request.email}", e)
                responseObserver.onError(
                    io.grpc.Status.UNAUTHENTICATED
                        .withDescription("Login failed: ${e.localizedMessage}")
                        .asRuntimeException()
                )
            }
        }
    }

    override fun getProfile(
        request: ProfileRequest,
        responseObserver: StreamObserver<ProfileResponse>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logger.info("Received gRPC GetProfile request")

                // Get profile logic
                val email = jwt.verifyTokenAndGetEmail(request.token)
                    ?: throw IllegalArgumentException("Invalid token: Unable to extract email")

                val profile: ProfileType = profileService.getProfileByEmail(email)
                    ?: throw IllegalArgumentException("Profile not found for email: $email")

                val response = ProfileResponse.newBuilder()
                    .setName(profile.name)
                    .setEmail(profile.email)
                    .build()
                responseObserver.onNext(response)
                responseObserver.onCompleted()
                logger.info("gRPC GetProfile response sent for token")
            } catch (e: Exception) {
                logger.error("gRPC GetProfile failed", e)
                responseObserver.onError(
                    io.grpc.Status.UNAUTHENTICATED
                        .withDescription("Failed to fetch profile: ${e.localizedMessage}")
                        .asRuntimeException()
                )
            }
        }
    }
}
