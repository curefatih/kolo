# OpenAI Streaming Response Usage Example

This document demonstrates how to use the OpenAI streaming response functionality with data buffering to handle partial chunks.

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
