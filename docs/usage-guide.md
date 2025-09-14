# Kolo Usage Guide

This guide provides comprehensive examples of how to use the Kolo library to convert between different LLM providers.

## Table of Contents

- [Kolo Usage Guide](#kolo-usage-guide)
  - [Table of Contents](#table-of-contents)
  - [Getting Started](#getting-started)
  - [Basic Usage](#basic-usage)
  - [Bidirectional Conversion](#bidirectional-conversion)
  - [Provider Discovery](#provider-discovery)
  - [Dynamic Provider Creation](#dynamic-provider-creation)
  - [Streaming Support](#streaming-support)
  - [Error Handling](#error-handling)
  - [Advanced Examples](#advanced-examples)
    - [Custom Request Types](#custom-request-types)
    - [Batch Processing](#batch-processing)
    - [Integration with HTTP Clients](#integration-with-http-clients)
  - [Best Practices](#best-practices)
  - [Troubleshooting](#troubleshooting)
    - [Common Issues](#common-issues)
    - [Getting Help](#getting-help)

## Getting Started

First, add the Kolo library to your project dependencies:

```kotlin
dependencies {
    implementation("com.fatihcure.kolo:providers:1.1.0")
    implementation("com.fatihcure.kolo:normalizers:1.1.0")
    implementation("com.fatihcure.kolo:transformers:1.1.0")
}
```

## Basic Usage

The simplest way to use Kolo is with the generic provider system:

```kotlin
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIMessage
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.providers.KoloProvider

fun basicExample() {
    val provider = KoloProvider()

    // Create a Kolo instance using generic types
    val kolo = provider.createKolo<OpenAIRequest, AnthropicRequest>()

    // Create an OpenAI-style request
    val openAIRequest = OpenAIRequest(
        model = "gpt-3.5-turbo",
        messages = listOf(
            OpenAIMessage(
                role = "system",
                content = "You are a helpful assistant."
            ),
            OpenAIMessage(
                role = "user",
                content = "Hello, how are you?"
            )
        ),
        temperature = 0.7,
        maxTokens = 100
    )

    // Convert to Anthropic format
    val anthropicRequest = kolo.convertRequest(openAIRequest)

    println("OpenAI Request: $openAIRequest")
    println("Converted to Anthropic: $anthropicRequest")
}
```

## Bidirectional Conversion

For applications that need to convert in both directions, use the bidirectional Kolo instance:

```kotlin
fun bidirectionalExample() {
    val provider = KoloProvider()

    // Create a bidirectional Kolo instance
    val bidirectionalKolo = provider.createBidirectionalKolo<OpenAIRequest, AnthropicRequest>()

    // Create an OpenAI-style request
    val openAIRequest = OpenAIRequest(
        model = "gpt-3.5-turbo",
        messages = listOf(
            OpenAIMessage(
                role = "system",
                content = "You are a helpful assistant."
            ),
            OpenAIMessage(
                role = "user",
                content = "Hello, how are you?"
            )
        ),
        temperature = 0.7,
        maxTokens = 100
    )

    // Convert to Anthropic format
    val anthropicRequest = bidirectionalKolo.convertRequest(openAIRequest)
    println("OpenAI -> Anthropic: $anthropicRequest")

    // Convert back to OpenAI format
    val backToOpenAI = bidirectionalKolo.convertResponse(anthropicRequest)
    println("Anthropic -> OpenAI: $backToOpenAI")
}
```

## Provider Discovery

You can discover what conversions are possible at runtime:

```kotlin
fun providerDiscoveryExample() {
    val provider = KoloProvider()

    // Check what conversions are possible
    val canConvert = provider.canConvert(OpenAIRequest::class, AnthropicRequest::class)
    println("Can convert OpenAI to Anthropic: $canConvert")

    val canConvertBidirectional = provider.canConvertBidirectional(OpenAIRequest::class, AnthropicRequest::class)
    println("Can convert bidirectionally: $canConvertBidirectional")

    // Get all possible targets for OpenAI
    val possibleTargets = provider.getPossibleTargets(OpenAIRequest::class)
    println("Possible targets for OpenAI: ${possibleTargets.map { it.simpleName }}")

    // Get all possible sources for Anthropic
    val possibleSources = provider.getPossibleSources(AnthropicRequest::class)
    println("Possible sources for Anthropic: ${possibleSources.map { it.simpleName }}")

    // Get all conversion pairs
    val allPairs = provider.getAllConversionPairs()
    println("All possible conversion pairs: ${allPairs.map { "${it.first.simpleName} -> ${it.second.simpleName}" }}")
}
```

## Dynamic Provider Creation

Create providers dynamically based on runtime types:

```kotlin
fun dynamicProviderExample() {
    val provider = KoloProvider()

    // Create providers dynamically based on runtime types
    val sourceType = OpenAIRequest::class
    val targetType = AnthropicRequest::class

    if (provider.canConvert(sourceType, targetType)) {
        val kolo = provider.createKolo(sourceType, targetType)
        println("Created Kolo instance for ${sourceType.simpleName} -> ${targetType.simpleName}")

        // Use the kolo instance...
    } else {
        println("Cannot convert from ${sourceType.simpleName} to ${targetType.simpleName}")
    }
}
```


## Streaming Support

Kolo supports streaming responses for real-time applications:

```kotlin
fun streamingExample() {
    val provider = KoloProvider()
    val bidirectionalKolo = provider.createBidirectionalKolo<OpenAIRequest, AnthropicRequest>()

    // In a real scenario, you would:
    // 1. Receive a streaming response from Anthropic
    // 2. Convert it to OpenAI format using bidirectionalKolo.convertStreamingResponse()
    // 3. Stream the converted response to your client

    println("Streaming conversion works with Flow<StreamEvent> instead of single objects")
}
```

## Error Handling

The library provides robust error handling for various scenarios:

```kotlin
fun errorHandlingExample() {
    val provider = KoloProvider()
    val bidirectionalKolo = provider.createBidirectionalKolo<OpenAIRequest, AnthropicRequest>()

    try {
        // Create a request in OpenAI format
        val openAIRequest = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(
                    role = "user",
                    content = "Hello, how are you?"
                )
            ),
            temperature = 0.7,
            maxTokens = 100
        )

        // Convert to Anthropic format
        val anthropicRequest = bidirectionalKolo.convertRequest(openAIRequest)

        println("OpenAI Request: $openAIRequest")
        println("Converted to Anthropic: $anthropicRequest")
    } catch (e: Exception) {
        println("Error during conversion: ${e.message}")
        // Handle the error appropriately
    }
}
```

## Advanced Examples

### Custom Request Types

You can work with custom request types by ensuring they implement the appropriate interfaces:

```kotlin
// Define your custom request type
data class CustomRequest(
    val prompt: String,
    val maxTokens: Int,
    val temperature: Double
)

// Use with Kolo (assuming proper normalizers are implemented)
fun customRequestExample() {
    val provider = KoloProvider()
    
    // Check if conversion is supported
    if (provider.canConvert(CustomRequest::class, AnthropicRequest::class)) {
        val kolo = provider.createKolo<CustomRequest, AnthropicRequest>()
        
        val customRequest = CustomRequest(
            prompt = "Tell me a joke",
            maxTokens = 50,
            temperature = 0.8
        )
        
        val anthropicRequest = kolo.convertRequest(customRequest)
        println("Converted custom request: $anthropicRequest")
    }
}
```

### Batch Processing

Process multiple requests efficiently:

```kotlin
fun batchProcessingExample() {
    val provider = KoloProvider()
    val kolo = provider.createKolo<OpenAIRequest, AnthropicRequest>()

    val requests = listOf(
        OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(OpenAIMessage(role = "user", content = "Hello 1")),
            temperature = 0.7,
            maxTokens = 100
        ),
        OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(OpenAIMessage(role = "user", content = "Hello 2")),
            temperature = 0.7,
            maxTokens = 100
        )
    )

    // Convert all requests
    val convertedRequests = requests.map { kolo.convertRequest(it) }
    
    println("Converted ${convertedRequests.size} requests")
}
```

### Integration with HTTP Clients

Example of integrating with an HTTP client:

```kotlin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async

fun httpIntegrationExample() {
    val provider = KoloProvider()
    val kolo = provider.createKolo<OpenAIRequest, AnthropicRequest>()

    runBlocking {
        val openAIRequest = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Hello")
            ),
            temperature = 0.7,
            maxTokens = 100
        )

        // Convert request
        val anthropicRequest = kolo.convertRequest(openAIRequest)

        // Send to Anthropic API (pseudo-code)
        // val response = httpClient.post("https://api.anthropic.com/v1/messages") {
        //     contentType(ContentType.Application.Json)
        //     setBody(anthropicRequest)
        // }

        // Convert response back
        // val openAIResponse = kolo.convertResponse(anthropicResponse)
    }
}
```

## Best Practices

1. **Use Generic Types**: Prefer the generic `createKolo<Source, Target>()` method for better type safety.

2. **Check Capabilities**: Always check if a conversion is possible using `canConvert()` before attempting it.

3. **Handle Errors**: Wrap conversion calls in try-catch blocks to handle potential errors gracefully.

4. **Use Bidirectional When Needed**: If you need to convert in both directions, use `createBidirectionalKolo()`.

5. **Cache Provider Instances**: Create provider instances once and reuse them for better performance.

6. **Validate Input**: Ensure your input requests are valid before conversion.

## Troubleshooting

### Common Issues

1. **UnsupportedConversionException**: This occurs when trying to convert between unsupported types. Check available conversions using `getAllConversionPairs()`.

2. **InvalidRequestException**: This occurs when the input request is malformed. Validate your request structure before conversion.

3. **Missing Dependencies**: Ensure all required modules are included in your dependencies.

### Getting Help

If you encounter issues:

1. Check the [API Reference](api-reference.md) for detailed method documentation if you need it
2. Verify your request format matches the expected structure
3. Ensure all required dependencies are included
