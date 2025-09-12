/*
 * Root build file for kolo project
 */

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("com.diffplug.spotless") version "6.25.0"
}

allprojects {
    group = "com.fatihcure.kolo"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    
    spotless {
        kotlin {
            target("**/*.kt")
            ktlint("1.2.1")
                .editorConfigOverride(mapOf(
                    "indent_size" to "4",
                    "continuation_indent_size" to "4",
                    "disabled_rules" to "no-wildcard-imports"
                ))
        }
        
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint("1.2.1")
                .editorConfigOverride(mapOf(
                    "indent_size" to "4",
                    "continuation_indent_size" to "4"
                ))
        }
        
        format("misc") {
            target("**/*.md", "**/.gitignore")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}