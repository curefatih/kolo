# Kolo Usage Guide

Kolo is a powerful library for converting between different LLM providers using an intermediate format. This guide shows how to migrate your existing streaming request handling code to use Kolo for seamless provider conversion.

## Table of Contents

1. [Overview](#overview)
2. [Migration from Manual Streaming](#migration-from-manual-streaming)
3. [Basic Usage](#basic-usage)
4. [Streaming Conversion](#streaming-conversion)
5. [Advanced Examples](#advanced-examples)
6. [Error Handling](#error-handling)

## Overview

Kolo provides a unified interface for converting between different LLM providers (OpenAI, Anthropic, etc.) by using an intermediate format. This eliminates the need for manual parsing and transformation of streaming data. The API is designed to be simple and intuitive, using provider classes for registration and conversion.

### Key Benefits

- **Unified Interface**: Single API for all provider conversions
- **Automatic Parsing**: Handles SSE format parsing automatically
- **Type Safety**: Full type safety with Kotlin generics
- **Error Handling**: Built-in error handling and recovery
- **Streaming Support**: Native support for streaming responses
- **Provider Class Registration**: Simple registration using provider classes instead of request types

## Migration from Manual Streaming

### Before: Manual Streaming Implementation

Here's how you might currently handle streaming requests manually:

```kotlin
private fun handleStreamingRequest(
    request: OpenAIRequest
): ResponseEntity<Flux<ServerSentEvent<String>>> {
    // Call OpenAI API with streaming - get raw SSE data
    val sseStream =
        webClient
            .post()
            .uri("/v1/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(String::class.java) // gives streaming chunks of text
            .flatMapIterable { chunk ->
                // Split by newlines and return all lines
                chunk.split("\n")
            }
            .filter { it.isNotEmpty() } // filter out empty lines
            .filter { line ->
                // Handle both SSE format (data: prefix) and raw JSON format
                when {
                    line.startsWith("data: ") -> !line.contains("[DONE]")
                    line.startsWith("{") && line.endsWith("}") -> true // raw JSON
                    else -> false
                }
            }
            .map { line ->
                // Remove SSE prefix if present, otherwise return as-is
                if (line.startsWith("data: ")) {
                    line.removePrefix("data: ")
                } else {
                    line
                }
            }
            .map { ServerSentEvent.builder(it).build() } // wrap in SSE format
            .onErrorResume { error: Throwable ->
                Flux.just(ServerSentEvent.builder("Error: ${error.message}").build())
            }

    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .header("Cache-Control", "no-cache")
        .header("Connection", "keep-alive")
        .body(sseStream)
}
```

### After: Using Kolo for Streaming

With Kolo, the same functionality becomes much simpler:

```kotlin
@Service
class StreamingService {
    
    private val koloProvider = KoloProvider()
    private val kolo = koloProvider.createKolo<
        OpenAIRequest, 
        OpenAIResponse, 
        OpenAIStreamEvent, 
        OpenAIError,
        AnthropicRequest, 
        AnthropicResponse, 
        AnthropicStreamEvent, 
        AnthropicError
    >(OpenAIProvider::class, AnthropicProvider::class)
    
    private fun handleStreamingRequest(
        request: OpenAIRequest
    ): ResponseEntity<Flux<ServerSentEvent<String>>> {
        // Convert request to target format
        val targetRequest = kolo.convertSourceRequestToTarget(request)
        
        // Get raw streaming data from target provider
        val rawStream = webClient
            .post()
            .uri("/v1/messages")
            .bodyValue(targetRequest)
            .retrieve()
            .bodyToFlux(String::class.java)
            .asFlow() // Convert to Kotlin Flow
        
        // Use Kolo to process streaming data
        val processedStream = kolo.processSourceStreamingToTargetStreaming(rawStream)
            .asFlux() // Convert back to Reactor Flux
            .map { streamEvent ->
                // Convert stream event to SSE format
                val json = kolo.objectMapper.writeValueAsString(streamEvent)
                ServerSentEvent.builder(json).build()
            }
            .onErrorResume { error: Throwable ->
                Flux.just(ServerSentEvent.builder("Error: ${error.message}").build())
            }

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")
            .body(processedStream)
    }
}
```

## Basic Usage

### 1. Provider Registration

Kolo uses provider class registration, which makes it simple to work with different LLM providers. Providers are registered by their class types rather than request types:

```kotlin
// Providers are automatically registered when using KoloProvider
val koloProvider = KoloProvider() // This registers OpenAIProvider and AnthropicProvider

// For custom providers, register them manually
val customProvider = CustomProvider()
GlobalProviderAutoRegistration.registerProvider(CustomProvider::class, customProvider)
```

### 2. Setting Up Kolo

First, create a Kolo instance with your desired source and target providers. Note that we now use provider classes instead of request types:

```kotlin
// Using the KoloProvider helper
val koloProvider = KoloProvider()
val kolo = koloProvider.createKolo<
    OpenAIRequest, 
    OpenAIResponse, 
    OpenAIStreamEvent, 
    OpenAIError,
    AnthropicRequest, 
    AnthropicResponse, 
    AnthropicStreamEvent, 
    AnthropicError
>(OpenAIProvider::class, AnthropicProvider::class)

// Or using the builder pattern
val kolo = kolo<OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError, 
                AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError> {
    withSourceProvider(OpenAIProvider())
    withTargetProvider(AnthropicProvider())
}
```

### 3. Converting Requests

```kotlin
// Convert a request from source to target format
val sourceRequest = OpenAIRequest(
    model = "gpt-4",
    messages = listOf(OpenAIMessage(role = "user", content = "Hello"))
)

val targetRequest = kolo.convertSourceRequestToTarget(sourceRequest)
```

### 4. Converting Responses

```kotlin
// Convert a response from target to source format
val targetResponse = AnthropicResponse(/* ... */)
val sourceResponse = kolo.convertTargetResponseToSource(targetResponse)
```

## Streaming Conversion

### Converting Streaming Data

Kolo handles the complexity of streaming data conversion automatically:

```kotlin
// Get raw streaming data from your HTTP client
val rawStream = webClient
    .post()
    .uri("/api/endpoint")
    .bodyValue(request)
    .retrieve()
    .bodyToFlux(String::class.java)
    .asFlow()

// Use Kolo to process the streaming data
val processedStream = kolo.processSourceStreamingToTargetStreaming(rawStream)
    .asFlux()
    .map { streamEvent ->
        // Convert to your desired output format
        ServerSentEvent.builder(streamEvent.toString()).build()
    }
```

### Working with Different Providers

Kolo supports conversion between any registered providers:

```kotlin
// OpenAI to Anthropic
val openaiToAnthropic = koloProvider.createKolo<
    OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError,
    AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError
>(OpenAIProvider::class, AnthropicProvider::class)

// Anthropic to OpenAI
val anthropicToOpenAI = koloProvider.createKolo<
    AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError,
    OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError
>(AnthropicProvider::class, OpenAIProvider::class)
```

## Advanced Examples

### Custom Provider Integration

If you have a custom provider, you can integrate it with Kolo:

```kotlin
class CustomProvider : StreamingProvider<CustomRequest, CustomResponse, CustomStreamEvent, CustomError> {
    // Implement the required methods
    override fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent> {
        // Your custom streaming processing logic
    }
    
    // ... other required methods
}

// Register and use your custom provider
val customProvider = CustomProvider()
GlobalProviderAutoRegistration.registerProvider(CustomProvider::class, customProvider)

val kolo = koloProvider.createKolo<
    CustomRequest, CustomResponse, CustomStreamEvent, CustomError,
    OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError
>(CustomProvider::class, OpenAIProvider::class)
```

### Error Handling in Streaming

Kolo provides built-in error handling for streaming operations:

```kotlin
val processedStream = kolo.processSourceStreamingToTargetStreaming(rawStream)
    .asFlux()
    .map { streamEvent ->
        ServerSentEvent.builder(streamEvent.toString()).build()
    }
    .onErrorResume { error ->
        // Handle conversion errors
        when (error) {
            is BufferSizeExceededException -> {
                Flux.just(ServerSentEvent.builder("Buffer size exceeded").build())
            }
            is JsonProcessingException -> {
                Flux.just(ServerSentEvent.builder("JSON parsing error").build())
            }
            else -> {
                Flux.just(ServerSentEvent.builder("Unknown error: ${error.message}").build())
            }
        }
    }
```

### Batch Processing

For non-streaming scenarios, you can process multiple requests in batch:

```kotlin
val requests = listOf(request1, request2, request3)
val convertedRequests = requests.map { kolo.convertSourceRequestToTarget(it) }

// Process all converted requests
val responses = convertedRequests.map { request ->
    // Call your target API
    webClient.post().bodyValue(request).retrieve().bodyToMono(TargetResponse::class.java)
}

// Convert responses back
val sourceResponses = responses.map { response ->
    kolo.convertTargetResponseToSource(response)
}
```

## Error Handling

Kolo provides comprehensive error handling:

### Common Error Types

- **BufferSizeExceededException**: When streaming buffer exceeds maximum size
- **JsonProcessingException**: When JSON parsing fails
- **ProviderNotFoundException**: When requested provider is not available
- **ConversionException**: When conversion between formats fails

### Error Recovery Strategies

```kotlin
val processedStream = kolo.processSourceStreamingToTargetStreaming(rawStream)
    .asFlux()
    .retry(3) // Retry up to 3 times
    .onErrorResume { error ->
        log.error("Streaming conversion failed", error)
        Flux.just(ServerSentEvent.builder("Service temporarily unavailable").build())
    }
    .doOnError { error ->
        // Log error for monitoring
        errorReporter.reportError(error)
    }
```

## Best Practices

1. **Reuse Kolo Instances**: Create Kolo instances once and reuse them
2. **Handle Errors Gracefully**: Always provide fallback behavior for streaming errors
3. **Monitor Performance**: Use appropriate buffer sizes for your use case
4. **Type Safety**: Leverage Kotlin's type system for compile-time safety
5. **Testing**: Test your streaming conversions with various data formats

## Conclusion

Kolo simplifies LLM provider conversion by handling the complexity of format transformation, streaming data processing, and error handling. This allows you to focus on your business logic while maintaining compatibility across different providers.

For more examples and advanced usage patterns, see the test files in the repository.
