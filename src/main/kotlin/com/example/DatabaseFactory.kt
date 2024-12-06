package com.example

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.Database as DB

object DatabaseFactory {
    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val dbUrl = appConfig.property("db.jdbcUrl").getString()
    private val dbUser = appConfig.property("db.dbUser").getString()
    private val dbPassword = appConfig.property("db.dbPassword").getString()
    private val driverClassName = appConfig.property("db.driverClassName").getString()

    fun init(testMigrationPath: String? = null) {
        try {
            DB.connect(hikari())
            val migrationPath = testMigrationPath ?: "classpath:db/migration"
            val flyway = Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPassword)
                .locations(migrationPath)
                .loggers("slf4j") // Ensure SLF4J is used for logging
                .load()
            flyway.migrate()
            println("Database migrations applied successfully.")
        } catch (e: Exception) {
            println("Failed to initialize the database: ${e.message}")
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = driverClassName
        config.jdbcUrl = dbUrl
        config.username = dbUser
        config.password = dbPassword
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

        // Test connection here if needed
        val dataSource = HikariDataSource(config)
        dataSource.connection.use {
            if (it.isValid(5)) {
                println("Database connection is valid!")
            }
        }

        return dataSource
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}
