package com.example.grpc

import com.example.service.userProfile.ProfileService
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import users.UserOuterClass.*
import users.UserServiceGrpc

class UserServiceImpl(
    private val profileService: ProfileService
) : UserServiceGrpc.UserServiceImplBase() {

    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    /**
     * Implement the GetAllUsers RPC
     */
    override fun getAllUsers(request: Empty, responseObserver: StreamObserver<UserList>) {
        // Launch a coroutine for the suspend function call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logger.info("Received request to fetch all users")

                // Call the suspend function to fetch users
                val users = profileService.getAllUsers()

                logger.debug("Fetched users from service: $users")

                // Convert the users to the gRPC User message
                val userList = UserList.newBuilder()
                    .addAllUsers(
                        users.map { profileType ->
                            User.newBuilder()
                                .setId(profileType.id.toString())
                                .setName(profileType.name)
                                .setEmail(profileType.email)
                                .build()
                        }
                    )
                    .build()

                // Send the response
                responseObserver.onNext(userList)
                responseObserver.onCompleted()
                logger.info("Successfully responded with user list")
            } catch (e: Exception) {
                logger.error("Error fetching users: ${e.localizedMessage}", e)
                responseObserver.onError(e)
            }
        }
    }
}
