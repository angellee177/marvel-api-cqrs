package com.example.service.userProfile

import com.example.DatabaseFactory.dbQuery
import com.example.models.ProfileType
import com.example.models.ProfileUser
import org.jetbrains.exposed.sql.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProfileService {
    private val logger: Logger = LoggerFactory.getLogger(ProfileService::class.java)

    /**
     * Get all User
     */
    suspend fun getAllUsers(): List<ProfileType> = dbQuery {
        logger.info("Fetching all users from the database")

        val users = ProfileUser.selectAll().map { toProfileType(it) }

        logger.debug("Fetched users: $users, with total users: ${users.count()}")
        users
    }

    /**
     * Get user profile by email
     */
    suspend fun getProfileByEmail(email: String): ProfileType? = dbQuery {
        logger.info("Fetching user with email: $email")

        val user = ProfileUser.selectAll()
            .where { (ProfileUser.email eq email) }
            .mapNotNull { toProfileType(it) }
            .singleOrNull()

        if (user != null) {
            logger.debug("Found user: $user")
        } else {
            logger.warn("No user found with email: $email")
        }

        user
    }


    /**
     * Register new user
     */
    suspend fun registerProfile(
        name: String,
        email: String,
        passwordHash: String
    ) = dbQuery {
        logger.info("Registering new user with email: $email")
        val result = ProfileUser.insert {
            it[ProfileUser.name] = name
            it[ProfileUser.email] = email
            it[password] = passwordHash
        }

        logger.debug("Inserted new user with email: $email, result: $result")
    }

    private fun toProfileType(row: ResultRow): ProfileType {
        val profile = ProfileType(
            id = row[ProfileUser.id],
            name = row[ProfileUser.name],
            email = row[ProfileUser.email],
            password = row[ProfileUser.password]
        )
        logger.debug("Mapped database row to ProfileType: $profile")

        return profile
    }
}