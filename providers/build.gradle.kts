/*
 * Providers module for kolo - contains provider-specific implementations
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // Core module dependency
    api(project(":core"))

    // Normalizers and transformers dependencies
    api(project(":normalizers"))
    api(project(":transformers"))

    // JSON processing
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)

    // Kotlin coroutines for streaming
    implementation(libs.kotlinx.coroutines.core)

    // HTTP client for API calls
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.jackson)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.ktor.client.mock)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
