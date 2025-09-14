# Publishing Guide

This guide explains how to publish the Kolo library to GitHub Packages.

## Overview

The project is configured to publish all modules to GitHub Packages:
- `com.fatihcure.kolo:core:1.2.0`
- `com.fatihcure.kolo:normalizers:1.2.0`
- `com.fatihcure.kolo:transformers:1.2.0`
- `com.fatihcure.kolo:providers:1.2.0`

## Prerequisites

1. **GitHub Personal Access Token**: Create a token with `write:packages` and `read:packages` permissions
2. **GitHub Username**: Your GitHub username

## Local Publishing

### Method 1: Using the publish script (Recommended)

```bash
# Set your GitHub credentials
export GITHUB_TOKEN=your_personal_access_token
export GITHUB_USERNAME=your_username

# Run the publish script
./scripts/publish-local.sh
```

### Method 2: Using Gradle directly

```bash
# Set your GitHub credentials
export GITHUB_TOKEN=your_personal_access_token
export GITHUB_USERNAME=your_username

# Build and publish
./gradlew clean build test publish
```

### Method 3: Using gradle.properties.local

Create a `gradle.properties.local` file (this file is gitignored):

```properties
gpr.user=your_username
gpr.key=your_personal_access_token
```

Then run:
```bash
./gradlew clean build test publish
```

## Automated Publishing

The project includes a GitHub Actions workflow that automatically publishes when you create a Git tag:

### Publishing a new version

1. **Update version** in `build.gradle.kts`:
   ```kotlin
   allprojects {
       group = "com.fatihcure.kolo"
       version = "1.2.0"  // Update version
       // ...
   }
   ```

2. **Commit and push changes**:
   ```bash
   git add .
   git commit -m "feat: release version 1.2.0"
   git push origin main
   ```

3. **Create and push a tag**:
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```

4. **The GitHub Action will automatically**:
   - Run tests
   - Build the project
   - Publish to GitHub Packages
   - Create a GitHub release

## Using the Published Packages

### In Gradle projects

Add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/curefatih/kolo")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_TOKEN")
        }
    }
}

dependencies {
    implementation("com.fatihcure.kolo:core:1.2.0")
    implementation("com.fatihcure.kolo:normalizers:1.2.0")
    implementation("com.fatihcure.kolo:transformers:1.2.0")
    implementation("com.fatihcure.kolo:providers:1.2.0")
}
```

### In Maven projects

Add to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/fatihcure/kolo</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.fatihcure.kolo</groupId>
        <artifactId>core</artifactId>
        <version>1.2.0</version>
    </dependency>
    <dependency>
        <groupId>com.fatihcure.kolo</groupId>
        <artifactId>normalizers</artifactId>
        <version>1.2.0</version>
    </dependency>
    <dependency>
        <groupId>com.fatihcure.kolo</groupId>
        <artifactId>transformers</artifactId>
        <version>1.2.0</version>
    </dependency>
    <dependency>
        <groupId>com.fatihcure.kolo</groupId>
        <artifactId>providers</artifactId>
        <version>1.2.0</version>
    </dependency>
</dependencies>
```

## Troubleshooting

### Authentication Issues

- Ensure your GitHub token has the correct permissions (`write:packages`, `read:packages`)
- Check that your username is correct
- Verify the token hasn't expired

### Build Issues

- Run `./gradlew clean` before publishing
- Ensure all tests pass with `./gradlew test`
- Check that the version number is valid (semantic versioning)

### Publishing Issues

- Check the GitHub Actions logs for detailed error messages
- Ensure the repository has the correct permissions
- Verify that the package name doesn't conflict with existing packages

## Package Information

- **Repository**: https://github.com/curefatih/kolo
- **Packages**: https://github.com/curefatih/kolo/packages
- **License**: MIT
- **Java Version**: 17+
