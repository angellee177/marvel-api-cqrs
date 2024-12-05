import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("com.google.protobuf") version "0.9.3" // Plugin for protobuf
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server dependencies
    implementation(libs.ktor.server.content.negotiation) // Content negotiation support
    implementation(libs.ktor.server.core) // Core Ktor server functionality
    implementation(libs.ktor.serialization.jackson) // JSON serialization using Jackson
    implementation(libs.ktor.server.auth) // Authentication framework for Ktor
    implementation(libs.ktor.server.auth.jwt) // JWT (JSON Web Token) authentication support
    implementation(libs.ktor.server.netty) // Netty engine for running the Ktor server
    implementation(libs.logback.classic) // Logging with Logback
    implementation(Libraries.logging) // CallLogging feature
    implementation(Libraries.ktorOkHttp)

    // Testing dependencies
    testImplementation(libs.ktor.server.test.host) // Testing utilities for Ktor applications
    testImplementation(libs.kotlin.test.junit) // JUnit integration for Kotlin tests

    // Database / persistence
    implementation(Libraries.postgresql) // PostgreSQL JDBC driver
    implementation(Libraries.flyway) // Database migrations with Flyway
    implementation(Libraries.hikari) // Connection pooling with HikariCP
    implementation(Libraries.sqlDelight) // SQLDelight for type-safe SQL

    // Kotlin core + standard requirements
    implementation(Libraries.kotlinBom) // Kotlin BOM for dependency alignment
    implementation(Libraries.serialization)

    // Exposed ORM dependencies
    implementation(Libraries.exposedCore) // Core DSL for building queries
    implementation(Libraries.exposedDao) // Optional: DAO support for Exposed
    implementation(Libraries.exposedJdbc) // JDBC driver integration for Exposed
    implementation(Libraries.exposedJavaTime) // Java Time API support in Exposed
    implementation(Libraries.dateTime) // Use the latest version

    // gRPC dependencies
    implementation(Libraries.grpcKotlinStub) // Kotlin gRPC support
    implementation(Libraries.grpcNettyShaded) // Netty-based gRPC server
    implementation(Libraries.grpcProtoBuf) // gRPC protobuf support
    implementation(Libraries.grpcStub) // Basic gRPC stubs

    // Protocol Buffers
    implementation(Libraries.protoBufKotlin)

    implementation(Libraries.koin)

    // Encode the password
    implementation(Libraries.bcrypt)
}

object Versions {
    const val kotlin = "2.0.0"
    const val logging = "2.0.0"
    const val ktorOkHttp = "3.0.0"
    const val serialization = "1.7.3"

    const val sqlDelight = "2.0.1"

    // DB Connection & persistence
    const val flyway = "11.0.0"
    const val hikari = "5.1.0"
    const val postgresql = "42.7.3"
    const val exposedCore = "0.56.0"
    const val exposedDao = "0.56.0"
    const val exposedJdbc = "0.56.0"
    const val exposedJavaTime = "0.56.0"
    const val koin = "4.0.0"
    const val dateTime = "0.6.1"
    const val bcrypt = "0.4"

    // gRPC connection
    const val grpcKotlinStub = "1.4.1"
    const val grpc = "1.68.2"

    const val protobufKotlin = "3.24.3"
}

internal val v = Versions

object Libraries {
    // DB connection
    const val postgresql = "org.postgresql:postgresql:${v.postgresql}" // PostgreSQL JDBC driver
    const val flyway = "org.flywaydb:flyway-core:${v.flyway}" // Database migration tool
    const val hikari = "com.zaxxer:HikariCP:${v.hikari}" // High-performance connection pooling
    const val sqlDelight = "app.cash.sqldelight:jdbc-driver:${v.sqlDelight}" // SQLDelight JDBC driver
    const val dateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${v.dateTime}"

    // Exposed ORM libraries
    const val exposedCore = "org.jetbrains.exposed:exposed-core:${v.exposedCore}" // Core DSL for building queries
    const val exposedDao = "org.jetbrains.exposed:exposed-dao:${v.exposedDao}" // DAO (Data Access Object) support
    const val exposedJdbc = "org.jetbrains.exposed:exposed-jdbc:${v.exposedJdbc}" // JDBC driver integration
    const val exposedJavaTime = "org.jetbrains.exposed:exposed-java-time:${v.exposedJavaTime}" // Java Time API support

    // Kotlin core + standard requirements
    const val kotlinBom = "org.jetbrains.kotlin:kotlin-bom:${v.kotlin}" // Kotlin BOM for dependency alignment
    const val logging = "io.ktor:ktor-server-call-logging:${v.logging}"
    const val ktorOkHttp = "io.ktor:ktor-client-okhttp:${v.ktorOkHttp}"
    const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:${v.serialization}"

    // gRPC dependencies
    const val grpcKotlinStub = "io.grpc:grpc-kotlin-stub:${v.grpcKotlinStub}" // Kotlin gRPC support
    const val grpcNettyShaded = "io.grpc:grpc-netty-shaded:${v.grpc}" // Netty-based gRPC server
    const val grpcProtoBuf = "io.grpc:grpc-protobuf:${v.grpc}" // gRPC protobuf support
    const val grpcStub = "io.grpc:grpc-stub:${v.grpc}" // Basic gRPC stubs

    // Protocol Buffers
    const val protoBufKotlin = "com.google.protobuf:protobuf-kotlin:${v.protobufKotlin}"

    // Encrypt password
    const val bcrypt = "org.mindrot:jbcrypt:${v.bcrypt}"

    const val koin = "io.insert-koin:koin-core:${v.koin}"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.3"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.57.2"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}






