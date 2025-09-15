/*
 * Root build file for kolo project
 */

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("com.diffplug.spotless") version "6.25.0"
}

allprojects {
    group = "com.fatihcure.kolo"
    version = "1.4.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    
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
    
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                
                pom {
                    name.set(project.name)
                    description.set("Kolo - A Kotlin library for LLM providers and transformation")
                    url.set("https://github.com/curefatih/kolo")
                    
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("curefatih")
                            name.set("Fatih Cure")
                            email.set("hello@fatihcure.com")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:git://github.com/curefatih/kolo.git")
                        developerConnection.set("scm:git:ssh://github.com:curefatih/kolo.git")
                        url.set("https://github.com/curefatih/kolo")
                    }
                }
            }
        }
        
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/curefatih/kolo")
                credentials {
                    username = System.getenv("GPR_USERNAME")
                    password = System.getenv("GPR_TOKEN")
                }
            }
        }
    }
}