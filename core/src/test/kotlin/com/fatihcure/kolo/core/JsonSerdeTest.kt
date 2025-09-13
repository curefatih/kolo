package com.fatihcure.kolo.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.StringReader

class JsonSerdeTest {

    @Test
    fun `should serialize and deserialize IntermittentRequest`() {
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(
                    role = MessageRole.USER,
                    content = "Hello, world!",
                    name = "user1",
                ),
                IntermittentMessage(
                    role = MessageRole.ASSISTANT,
                    content = "Hi there!",
                    name = "assistant1",
                ),
            ),
            model = "gpt-4",
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            frequencyPenalty = 0.1,
            presencePenalty = 0.2,
            stop = listOf("STOP", "END"),
            stream = true,
            metadata = mapOf("key1" to "value1", "key2" to 42),
        )

        val json = JsonSerde.toJson(request)
        val deserialized = JsonSerde.fromJson(json)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `should serialize and deserialize IntermittentRequest from InputStream`() {
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.USER, content = "Test message"),
            ),
            model = "gpt-3.5-turbo",
        )

        val json = JsonSerde.toJson(request)
        val inputStream = ByteArrayInputStream(json.toByteArray())
        val deserialized = JsonSerde.fromJson(inputStream)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `should serialize and deserialize IntermittentRequest from Reader`() {
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.SYSTEM, content = "System message"),
            ),
            model = "claude-3",
        )

        val json = JsonSerde.toJson(request)
        val reader = StringReader(json)
        val deserialized = JsonSerde.fromJson(reader)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `should serialize and deserialize IntermittentResponse`() {
        val response = IntermittentResponse(
            id = "response-123",
            model = "gpt-4",
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "This is a test response",
                    ),
                    finishReason = "stop",
                ),
            ),
            usage = IntermittentUsage(
                promptTokens = 10,
                completionTokens = 20,
                totalTokens = 30,
            ),
            metadata = mapOf("responseId" to "resp-123"),
        )

        val json = JsonSerde.toJson(response)
        val deserialized = JsonSerde.responseFromJson(json)

        assertThat(deserialized).isEqualTo(response)
    }

    @Test
    fun `should serialize and deserialize IntermittentStreamEvent MessageStart`() {
        val event = IntermittentStreamEvent.MessageStart(
            id = "stream-123",
            model = "gpt-4",
            metadata = mapOf("streamId" to "stream-123"),
        )

        val json = JsonSerde.toJson(event)
        val deserialized = JsonSerde.streamEventFromJson(json)

        assertThat(deserialized).isEqualTo(event)
    }

    @Test
    fun `should serialize and deserialize IntermittentStreamEvent MessageDelta`() {
        val event = IntermittentStreamEvent.MessageDelta(
            delta = IntermittentDelta(
                role = MessageRole.ASSISTANT,
                content = "Hello",
                name = "assistant",
            ),
            metadata = mapOf("deltaId" to "delta-123"),
        )

        val json = JsonSerde.toJson(event)
        val deserialized = JsonSerde.streamEventFromJson(json)

        assertThat(deserialized).isEqualTo(event)
    }

    @Test
    fun `should serialize and deserialize IntermittentStreamEvent MessageEnd`() {
        val event = IntermittentStreamEvent.MessageEnd(
            finishReason = "stop",
            usage = IntermittentUsage(
                promptTokens = 5,
                completionTokens = 10,
                totalTokens = 15,
            ),
            metadata = mapOf("endId" to "end-123"),
        )

        val json = JsonSerde.toJson(event)
        val deserialized = JsonSerde.streamEventFromJson(json)

        assertThat(deserialized).isEqualTo(event)
    }

    @Test
    fun `should serialize and deserialize IntermittentStreamEvent Error`() {
        val event = IntermittentStreamEvent.Error(
            error = IntermittentError(
                type = "invalid_request",
                message = "Invalid request format",
                code = "INVALID_REQUEST",
                param = "messages",
                metadata = mapOf("errorId" to "error-123"),
            ),
            metadata = mapOf("streamErrorId" to "stream-error-123"),
        )

        val json = JsonSerde.toJson(event)
        val deserialized = JsonSerde.streamEventFromJson(json)

        assertThat(deserialized).isEqualTo(event)
    }

    @Test
    fun `should serialize and deserialize IntermittentError`() {
        val error = IntermittentError(
            type = "rate_limit_exceeded",
            message = "Rate limit exceeded",
            code = "RATE_LIMIT",
            param = "requests_per_minute",
            metadata = mapOf("retryAfter" to 60),
        )

        val json = JsonSerde.toJson(error)
        val deserialized = JsonSerde.errorFromJson(json)

        assertThat(deserialized).isEqualTo(error)
    }

    @Test
    fun `should handle null values correctly`() {
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(
                    role = MessageRole.USER,
                    content = "Test",
                    name = null,
                    metadata = null,
                ),
            ),
            model = "gpt-4",
            temperature = null,
            maxTokens = null,
            topP = null,
            frequencyPenalty = null,
            presencePenalty = null,
            stop = null,
            stream = false,
            metadata = null,
        )

        val json = JsonSerde.toJson(request)
        val deserialized = JsonSerde.fromJson(json)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `should handle empty collections correctly`() {
        val request = IntermittentRequest(
            messages = emptyList(),
            model = "gpt-4",
            stop = emptyList(),
        )

        val json = JsonSerde.toJson(request)
        val deserialized = JsonSerde.fromJson(json)

        assertThat(deserialized).isEqualTo(request)
    }
}
