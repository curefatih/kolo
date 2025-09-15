#!/bin/bash

# Local publish script for testing GitHub Packages publishing
# Usage: ./scripts/publish-local.sh

set -e

echo "🚀 Publishing Kolo to GitHub Packages..."

# Check if GitHub token is set
if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ Error: GITHUB_TOKEN environment variable is not set"
    echo "Please set your GitHub Personal Access Token:"
    echo "export GITHUB_TOKEN=your_token_here"
    echo ""
    echo "To create a token:"
    echo "1. Go to GitHub Settings > Developer settings > Personal access tokens"
    echo "2. Generate a new token with 'write:packages' and 'read:packages' permissions"
    exit 1
fi

# Check if username is set
if [ -z "$GITHUB_USERNAME" ]; then
    echo "❌ Error: GITHUB_USERNAME environment variable is not set"
    echo "Please set your GitHub username:"
    echo "export GITHUB_USERNAME=your_username_here"
    exit 1
fi

echo "📦 Building project..."
./gradlew clean build --no-configuration-cache

echo "🧪 Running tests..."
./gradlew test --no-configuration-cache

echo "📤 Publishing to GitHub Packages..."
./gradlew publish --no-configuration-cache -Pgpr.user="$GITHUB_USERNAME" -Pgpr.key="$GITHUB_TOKEN"

# Versions are static for now
echo "✅ Successfully published to GitHub Packages!"
echo "📋 Packages published:"
echo "  - com.fatihcure.kolo:core:1.5.0"
echo "  - com.fatihcure.kolo:normalizers:1.5.0"
echo "  - com.fatihcure.kolo:transformers:1.5.0"
echo "  - com.fatihcure.kolo:providers:1.5.0"
echo ""
echo "🔗 View packages at: https://github.com/curefatih/kolo/packages"
