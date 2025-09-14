# Streaming Response Usage Examples

This document demonstrates how to use the Kolo library's streaming response functionality, including data buffering for partial chunks and bidirectional streaming conversion.

## Basic Usage

```kotlin
import com.fatihcure.kolo.providers.OpenAIProvider
import com.fatihcure.kolo.core.DefaultDataBuffer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create provider with default data buffer
    val provider = OpenAIProvider()
    
    // Simulate streaming data (in real usage, this would come from HTTP response)
    val streamingData = flowOf(
        """data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[{"index":0,"delta":{"role":"assistant","content":"","refusal":null},"logprobs":null,"finish_reason":null}],"usage":null,"obfuscation":"AAatV4uAp"}

""",
        """data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[{"index":0,"delta":{"content":"Once"},"logprobs":null,"finish_reason":null}],"usage":null,"obfuscation":"f9IQaNg"}

"""
    )
    
    // Process streaming data
    provider.processStreamingData(streamingData).collect { event ->
        when (event) {
            is IntermittentStreamEvent.MessageStart -> {
                println("Message started: ${event.id}")
            }
            is IntermittentStreamEvent.MessageDelta -> {
                println("Delta: ${event.delta.content}")
            }
            is IntermittentStreamEvent.MessageEnd -> {
                println("Message ended: ${event.finishReason}")
            }
            is IntermittentStreamEvent.Error -> {
                println("Error: ${event.error.message}")
            }
        }
    }
}
```

## Using Custom Data Buffer

```kotlin
import com.fatihcure.kolo.providers.OpenAIProvider
import com.fatihcure.kolo.providers.OpenAIProviderConfig
import com.fatihcure.kolo.core.DefaultDataBuffer

fun main() = runBlocking {
    // Create a factory for your custom data buffer
    val customBufferFactory = object : com.fatihcure.kolo.core.DataBufferFactory {
        override fun createBuffer(): com.fatihcure.kolo.core.DataBuffer {
            return DefaultDataBuffer() // Or your custom implementation
        }
    }
    
    // Create provider with custom configuration
    val config = OpenAIProviderConfig.withDataBufferFactory(customBufferFactory)
    val provider = OpenAIProvider(config)
    
    // Use the provider as before
    // ...
}
```

## Handling Partial Chunks

The data buffer automatically handles partial chunks. For example, if you receive:

```
data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[{"index":0,"delta":{"role":"assistant","content":"","refusal":null
```

The buffer will wait for the complete chunk (ending with `\n\n`) before processing it.

## Configuration Options

```kotlin
import com.fatihcure.kolo.providers.OpenAIProviderConfig
import com.fatihcure.kolo.core.DefaultDataBuffer
import com.fasterxml.jackson.databind.ObjectMapper

// Default configuration
val defaultConfig = OpenAIProviderConfig.default()

// Custom data buffer
val customBufferConfig = OpenAIProviderConfig.withDataBuffer(DefaultDataBuffer())

// Custom object mapper
val customMapper = ObjectMapper()
val customMapperConfig = OpenAIProviderConfig.withObjectMapper(customMapper)

// Both custom data buffer and object mapper
val fullCustomConfig = OpenAIProviderConfig.withCustom(DefaultDataBuffer(), customMapper)
```

## Error Handling

The streaming handler includes built-in error handling for parsing failures:

```kotlin
provider.processStreamingData(streamingData).collect { event ->
    when (event) {
        is IntermittentStreamEvent.Error -> {
            println("Streaming error: ${event.error.message}")
            // Handle error appropriately
        }
        // ... other event types
    }
}
```

## Bidirectional Streaming Conversion

The `BidirectionalKolo` class now supports streaming conversion between different providers:

```kotlin
import com.fatihcure.kolo.providers.KoloProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

fun bidirectionalStreamingExample() = runBlocking {
    val provider = KoloProvider()
    
    // Create a bidirectional Kolo instance with streaming support
    val bidirectionalKolo = provider.createBidirectionalKolo<OpenAIRequest, AnthropicRequest>()
    
    // Simulate receiving a streaming response from Anthropic
    val anthropicStream: Flow<AnthropicStreamEvent> = flowOf(
        // In real usage, this would come from the Anthropic API
        AnthropicStreamEvent(/* ... */)
    )
    
    // Convert the streaming response to OpenAI format
    val openAIStream: Flow<OpenAIStreamEvent> = bidirectionalKolo.convertStreamingResponse(anthropicStream)
    
    // Process the converted stream
    openAIStream.collect { event ->
        when (event) {
            is OpenAIStreamEvent.MessageStart -> println("OpenAI Message started: ${event.id}")
            is OpenAIStreamEvent.MessageDelta -> println("OpenAI Delta: ${event.delta.content}")
            is OpenAIStreamEvent.MessageEnd -> println("OpenAI Message ended: ${event.finishReason}")
            is OpenAIStreamEvent.Error -> println("OpenAI Error: ${event.error.message}")
        }
    }
}
```

### Manual Streaming Transformer Configuration

You can also configure streaming transformers manually:

```kotlin
import com.fatihcure.kolo.core.bidirectionalKoloBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

fun manualStreamingExample() = runBlocking {
    val sourceNormalizer = /* your source normalizer */
    val targetNormalizer = /* your target normalizer */
    val sourceTransformer = /* your source transformer */
    val targetTransformer = /* your target transformer */
    val sourceStreamingTransformer = /* your source streaming transformer */
    val targetStreamingTransformer = /* your target streaming transformer */

    val bidirectionalKolo = bidirectionalKoloBuilder<String, String>()
        .withSourceNormalizer(sourceNormalizer)
        .withTargetNormalizer(targetNormalizer)
        .withSourceTransformer(sourceTransformer)
        .withTargetTransformer(targetTransformer)
        .withSourceStreamingTransformer(sourceStreamingTransformer)
        .withTargetStreamingTransformer(targetStreamingTransformer)
        .build()

    // Use streaming conversion
    val targetStream = flowOf("Hello", " World", "!")
    val sourceStream = bidirectionalKolo.convertStreamingResponse(targetStream)
    
    sourceStream.collect { content ->
        println("Converted: $content")
    }
}
```

### Error Handling for Streaming

```kotlin
fun streamingErrorHandlingExample() = runBlocking {
    val provider = KoloProvider()
    val bidirectionalKolo = provider.createBidirectionalKolo<OpenAIRequest, AnthropicRequest>()
    
    try {
        val anthropicStream: Flow<AnthropicStreamEvent> = flowOf(/* ... */)
        val openAIStream = bidirectionalKolo.convertStreamingResponse(anthropicStream)
        
        openAIStream.collect { event ->
            // Process events
        }
    } catch (e: IllegalArgumentException) {
        when {
            e.message?.contains("Source streaming transformer") == true -> {
                println("Source streaming transformer is required but not available")
            }
            e.message?.contains("Target streaming transformer") == true -> {
                println("Target streaming transformer is required but not available")
            }
            else -> {
                println("Streaming conversion error: ${e.message}")
            }
        }
    }
}
```

## Integration with HTTP Client

Here's an example of how you might integrate this with an HTTP client:

```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun streamFromHttpClient(url: String): Flow<String> = flow {
    // This is a simplified example - in practice you'd use a real HTTP client
    // like Ktor, OkHttp, or similar
    val response = httpClient.get(url) {
        headers {
            append("Accept", "text/event-stream")
        }
    }
    
    response.bodyAsChannel().toFlow().collect { chunk ->
        emit(chunk.decodeToString())
    }
}

fun main() = runBlocking {
    val provider = OpenAIProvider()
    val httpStream = streamFromHttpClient("https://api.openai.com/v1/chat/completions")
    
    provider.processStreamingData(httpStream).collect { event ->
        // Process streaming events
    }
}
```

### Complete HTTP Integration with Streaming Conversion

```kotlin
import com.fatihcure.kolo.providers.KoloProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

fun completeHttpStreamingExample() = runBlocking {
    val provider = KoloProvider()
    val bidirectionalKolo = provider.createBidirectionalKolo<OpenAIRequest, AnthropicRequest>()
    
    // Stream from Anthropic API
    val anthropicStream = streamFromHttpClient("https://api.anthropic.com/v1/messages/stream")
    
    // Convert to OpenAI format
    val openAIStream = bidirectionalKolo.convertStreamingResponse(anthropicStream)
    
    // Stream to client in OpenAI format
    openAIStream.collect { event ->
        // Send to client
        println("Sending to client: $event")
    }
}
```

## Auto-Registration of Streaming Transformers

Streaming transformers can be automatically registered using the `@AutoRegisterStreamingTransformer` annotation. This allows the system to automatically discover and register streaming transformers without manual registration.

### Example: Auto-Registered Streaming Transformer

```kotlin
import com.fatihcure.kolo.core.AutoRegisterStreamingTransformer
import com.fatihcure.kolo.core.StreamingTransformer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define your custom stream event type
data class CustomStreamEvent(
    val id: String,
    val content: String,
    val isComplete: Boolean = false
)

// Mark the transformer for auto-registration
@AutoRegisterStreamingTransformer(CustomStreamEvent::class)
class CustomStreamingTransformer : StreamingTransformer<CustomStreamEvent> {
    
    override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<CustomStreamEvent> {
        return stream.map { event ->
            when (event) {
                is IntermittentStreamEvent.MessageStart -> CustomStreamEvent(
                    id = event.id,
                    content = ""
                )
                is IntermittentStreamEvent.MessageDelta -> CustomStreamEvent(
                    id = event.id,
                    content = event.delta.content ?: ""
                )
                is IntermittentStreamEvent.MessageEnd -> CustomStreamEvent(
                    id = event.id,
                    content = "",
                    isComplete = true
                )
                is IntermittentStreamEvent.Error -> CustomStreamEvent(
                    id = "error",
                    content = event.error.message
                )
            }
        }
    }
}
```

### Manual Registration

You can also manually register streaming transformers:

```kotlin
import com.fatihcure.kolo.core.GlobalProviderAutoRegistration

// Register a single streaming transformer
GlobalProviderAutoRegistration.registerStreamingTransformer(
    CustomStreamEvent::class,
    CustomStreamingTransformer()
)

// Register multiple streaming transformers
GlobalProviderAutoRegistration.registerStreamingTransformers(
    CustomStreamEvent::class to CustomStreamingTransformer(),
    AnotherStreamEvent::class to AnotherStreamingTransformer()
)
```

### Using Auto-Registered Streaming Transformers

Once registered (either automatically or manually), streaming transformers can be used in bidirectional Kolo:

```kotlin
import com.fatihcure.kolo.core.GlobalProviderRegistry

fun useAutoRegisteredTransformers() = runBlocking {
    // Check if a streaming transformer is available
    val hasCustomTransformer = GlobalProviderRegistry.hasStreamingTransformer(CustomStreamEvent::class)
    
    if (hasCustomTransformer) {
        val transformer = GlobalProviderRegistry.getStreamingTransformer<CustomStreamEvent>(CustomStreamEvent::class)
        // Use the transformer...
    }
}
```
