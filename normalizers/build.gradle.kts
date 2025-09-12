/*
 * Normalizers module for kolo - contains normalizers for different LLM providers
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

    // JSON processing
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)

    // Kotlin coroutines for streaming
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
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
