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
- **Full Compile-Time Safety**: Provider-instance approach eliminates runtime casting and type erasure issues
- **Cleaner API**: No need to specify complex type parameters manually

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
    private val openAIProvider = OpenAIProvider()
    private val anthropicProvider = AnthropicProvider()
    private val kolo = koloProvider.createKolo(openAIProvider, anthropicProvider)
    
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
            .bodyToFlux(DataBuffer::class.java)
            .map { buffer ->
                val bytes = ByteArray(buffer.readableByteCount())
                buffer.read(bytes)
                buffer.release()
                String(bytes, StandardCharsets.UTF_8)
            }
            .asFlow() // Convert to Kotlin Flow
        
        // Use Kolo to process streaming data
        val processedStream = kolo.processRawStreamingThroughConversion(rawStream)
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

First, create a Kolo instance with your desired source and target providers. You can use either the provider-instance approach (recommended) or the KClass-based approach:

```kotlin
// Recommended: Provider-instance approach (full compile-time safety)
val koloProvider = KoloProvider()
val openAIProvider = OpenAIProvider()
val anthropicProvider = AnthropicProvider()

// Full compile-time safety, no casting needed
val kolo = koloProvider.createKolo(openAIProvider, anthropicProvider)

// Alternative: KClass-based approach
val koloClassBased = koloProvider.createKolo<
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
val koloBuilder = kolo<OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError, 
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

### 5. Accessing Type Information

With the provider-instance approach, you can access type information at runtime:

```kotlin
val openAIProvider = OpenAIProvider()

// Access type information
println("Request Type: ${openAIProvider.requestType}")
println("Response Type: ${openAIProvider.responseType}")
println("Streaming Type: ${openAIProvider.streamingResponseType}")
println("Error Type: ${openAIProvider.errorType}")

// Use type information for dynamic operations
val requestType: KClass<out OpenAIRequest> = openAIProvider.requestType
val responseType: KClass<out OpenAIResponse> = openAIProvider.responseType
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
    .bodyToFlux(DataBuffer::class.java)
    .map { buffer ->
        val bytes = ByteArray(buffer.readableByteCount())
        buffer.read(bytes)
        buffer.release()
        String(bytes, StandardCharsets.UTF_8)
    }
    .asFlow()

// Use Kolo to process the streaming data
val processedStream = kolo.processRawStreamingThroughConversion(rawStream)
    .asFlux()
    .map { streamEvent ->
        // Convert to your desired output format
        ServerSentEvent.builder(streamEvent.toString()).build()
    }
```

### Working with Different Providers

Kolo supports conversion between any registered providers:

```kotlin
// Recommended: Provider-instance approach
val openAIProvider = OpenAIProvider()
val anthropicProvider = AnthropicProvider()

// OpenAI to Anthropic
val openaiToAnthropic = koloProvider.createKolo(openAIProvider, anthropicProvider)

// Anthropic to OpenAI
val anthropicToOpenAI = koloProvider.createKolo(anthropicProvider, openAIProvider)

// Alternative: KClass-based approach
val openaiToAnthropicClass = koloProvider.createKolo<
    OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError,
    AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError
>(OpenAIProvider::class, AnthropicProvider::class)

val anthropicToOpenAIClass = koloProvider.createKolo<
    AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError,
    OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError
>(AnthropicProvider::class, OpenAIProvider::class)
```

## Advanced Examples

### Custom Provider Integration

If you have a custom provider, you can integrate it with Kolo:

```kotlin
class CustomProvider : StreamingProvider<CustomRequest, CustomResponse, CustomStreamEvent, CustomError> {
    // Type information for compile-time safety
    override val requestType: KClass<out CustomRequest> = CustomRequest::class
    override val responseType: KClass<out CustomResponse> = CustomResponse::class
    override val streamingResponseType: KClass<out CustomStreamEvent> = CustomStreamEvent::class
    override val errorType: KClass<out CustomError> = CustomError::class
    
    // Implement the required methods
    override fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent> {
        // Your custom streaming processing logic
    }
    
    // ... other required methods
}

// Register and use your custom provider
val customProvider = CustomProvider()
GlobalProviderAutoRegistration.registerProvider(CustomProvider::class, customProvider)

// Recommended: Provider-instance approach
val openAIProvider = OpenAIProvider()
val kolo = koloProvider.createKolo(customProvider, openAIProvider)

// Alternative: KClass-based approach
val koloClassBased = koloProvider.createKolo<
    CustomRequest, CustomResponse, CustomStreamEvent, CustomError,
    OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError
>(CustomProvider::class, OpenAIProvider::class)
```

### Error Handling in Streaming

Kolo provides built-in error handling for streaming operations:

```kotlin
val processedStream = kolo.processRawStreamingThroughConversion(rawStream)
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
val processedStream = kolo.processRawStreamingThroughConversion(rawStream)
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

## Provider-Instance vs KClass Approach

### Provider-Instance Approach (Recommended)

**Benefits:**
- **Full Compile-Time Safety**: No runtime casting or type erasure issues
- **Cleaner API**: No need to specify complex type parameters manually
- **Better IDE Support**: Full autocomplete and type checking
- **Runtime Type Information**: Access to provider type information at runtime
- **Easier to Use**: More intuitive and less error-prone

**Usage:**
```kotlin
val openAIProvider = OpenAIProvider()
val anthropicProvider = AnthropicProvider()
val kolo = koloProvider.createKolo(openAIProvider, anthropicProvider)
```

### KClass-Based Approach (Legacy)

**Benefits:**
- **Familiar**: Similar to existing reflection-based patterns
- **Flexible**: Can work with provider classes without instances

**Usage:**
```kotlin
val kolo = koloProvider.createKolo<
    OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError,
    AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError
>(OpenAIProvider::class, AnthropicProvider::class)
```

## Best Practices

1. **Use Provider-Instance Approach**: Prefer the provider-instance approach for better type safety and cleaner code
2. **Reuse Kolo Instances**: Create Kolo instances once and reuse them
3. **Handle Errors Gracefully**: Always provide fallback behavior for streaming errors
4. **Monitor Performance**: Use appropriate buffer sizes for your use case
5. **Type Safety**: Leverage Kotlin's type system for compile-time safety
6. **Testing**: Test your streaming conversions with various data formats

## Conclusion

Kolo simplifies LLM provider conversion by handling the complexity of format transformation, streaming data processing, and error handling. This allows you to focus on your business logic while maintaining compatibility across different providers.

For more examples and advanced usage patterns, see the test files in the repository.
