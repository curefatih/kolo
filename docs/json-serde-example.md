# JSON Serialization/Deserialization Example

This document demonstrates how to use the JSON serialization/deserialization capabilities in the Kolo library.

## Basic JSON Operations

### Serializing to JSON

```kotlin
import com.fatihcure.kolo.core.*

// Create an IntermittentRequest
val request = IntermittentRequest(
    messages = listOf(
        IntermittentMessage(
            role = MessageRole.USER,
            content = "Hello, world!"
        )
    ),
    model = "gpt-4",
    temperature = 0.7,
    maxTokens = 100
)

// Convert to JSON string
val json = JsonSerde.toJson(request)
println(json)
```

### Deserializing from JSON

```kotlin
val jsonString = """
{
    "messages": [
        {
            "role": "user",
            "content": "Hello, world!"
        }
    ],
    "model": "gpt-4",
    "temperature": 0.7,
    "maxTokens": 100
}
""".trimIndent()

val request = JsonSerde.fromJson(jsonString)
println(request.model) // "gpt-4"
```

## Using with Kolo Classes

The Kolo library now provides integrated JSON serialization/deserialization methods directly in the Kolo classes. Each Kolo instance has its own ObjectMapper configured with Kotlin support, making JSON operations seamless and type-safe.

### Available Methods

**Kolo Class:**
- `convertRequestFromJson<T>(json: String): TargetType` - Convert JSON to target format
- `convertRequestToJson(sourceRequest: SourceType): String` - Convert source to JSON

**BidirectionalKolo Class:**
- `convertRequestFromJson<T>(json: String): TargetType` - Convert JSON to target format
- `convertRequestToJson(sourceRequest: SourceType): String` - Convert source to JSON
- `convertResponseToJson(targetResponse: TargetType): String` - Convert target to JSON
- `convertResponseFromJson<T>(json: String): SourceType` - Convert JSON to source format
- `convertStreamingResponseToJson(targetStream: Flow<TargetType>): Flow<String>` - Convert streaming response to JSON
- `convertErrorToJson(targetError: TargetType): String` - Convert error to JSON
- `convertErrorFromJson<T>(json: String): SourceType` - Convert JSON error to source format

### Converting from JSON to Target Format

```kotlin
// Assuming you have a Kolo instance set up
val kolo = koloBuilder<String, AnthropicRequest>()
    .withSourceNormalizer(/* your normalizer */)
    .withTargetTransformer(/* your transformer */)
    .build()

val jsonRequest = """
"Hello from JSON"
""".trimIndent()

// Convert directly from JSON to target format
val anthropicRequest = kolo.convertRequestFromJson<String>(jsonRequest)
```

### Converting from Source Format to JSON

```kotlin
val sourceRequest = "Hello, world!"

// Convert from source format to JSON
val json = kolo.convertRequestToJson(sourceRequest)
println(json)
```

## Bidirectional Conversion

### Converting Response to JSON

```kotlin
val bidirectionalKolo = bidirectionalKoloBuilder<String, AnthropicResponse>()
    .withSourceNormalizer(/* your normalizer */)
    .withTargetNormalizer(/* your normalizer */)
    .withSourceTransformer(/* your transformer */)
    .withTargetTransformer(/* your transformer */)
    .build()

val anthropicResponse = AnthropicResponse(/* ... */)

// Convert response to JSON
val jsonResponse = bidirectionalKolo.convertResponseToJson(anthropicResponse)
```

### Converting JSON to Source Format

```kotlin
val jsonResponse = """
"Hello! How can I help you?"
""".trimIndent()

// Convert from JSON to source format
val sourceResponse = bidirectionalKolo.convertResponseFromJson<String>(jsonResponse)
```

## Streaming Events

### Serializing Stream Events

```kotlin
val streamEvent = IntermittentStreamEvent.MessageStart(
    id = "stream-123",
    model = "gpt-4"
)

val json = JsonSerde.toJson(streamEvent)
```

### Deserializing Stream Events

```kotlin
val json = """
{
    "type": "message_start",
    "id": "stream-123",
    "model": "gpt-4"
}
""".trimIndent()

val streamEvent = JsonSerde.streamEventFromJson(json)
```

## Error Handling

### Serializing Errors

```kotlin
val error = IntermittentError(
    type = "rate_limit_exceeded",
    message = "Rate limit exceeded",
    code = "RATE_LIMIT"
)

val json = JsonSerde.toJson(error)
```

### Deserializing Errors

```kotlin
val json = """
{
    "type": "rate_limit_exceeded",
    "message": "Rate limit exceeded",
    "code": "RATE_LIMIT"
}
""".trimIndent()

val error = JsonSerde.errorFromJson(json)
```

## Advanced Usage

### Custom ObjectMapper

```kotlin
// Access the underlying ObjectMapper for advanced configuration
val objectMapper = JsonSerde.objectMapper
objectMapper.configure(/* your configuration */)
```

### Reading from InputStream or Reader

```kotlin
// From InputStream
val inputStream = FileInputStream("request.json")
val request = JsonSerde.fromJson(inputStream)

// From Reader
val reader = FileReader("request.json")
val request = JsonSerde.fromJson(reader)
```

This JSON serialization/deserialization capability makes it easy to work with Kolo's intermittent format in JSON-based systems and APIs.
