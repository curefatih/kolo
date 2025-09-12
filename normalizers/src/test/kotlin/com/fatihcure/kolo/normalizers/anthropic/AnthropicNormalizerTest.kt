package com.fatihcure.kolo.normalizers.anthropic

import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.normalizers.TestUtils
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnthropicNormalizerTest {

    private lateinit var normalizer: AnthropicNormalizer

    @BeforeEach
    fun setUp() {
        normalizer = AnthropicNormalizer()
    }

    @Test
    fun `normalizeRequest should convert AnthropicRequest to IntermittentRequest`() {
        // Given
        val anthropicRequest = AnthropicRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(role = "user", content = "Hello, how are you?"),
                AnthropicMessage(role = "assistant", content = "I'm doing well, thank you!"),
            ),
            system = "You are a helpful assistant.",
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            stop = listOf("STOP"),
            stream = false,
        )

        // When
        val result = normalizer.normalizeRequest(anthropicRequest)

        // Then
        assertThat(result.model).isEqualTo("claude-3-sonnet-20240229")
        assertThat(result.temperature).isEqualTo(0.7)
        assertThat(result.maxTokens).isEqualTo(100)
        assertThat(result.topP).isEqualTo(0.9)
        assertThat(result.stop).isEqualTo(listOf("STOP"))
        assertThat(result.stream).isFalse()

        // Check messages
        assertThat(result.messages).hasSize(3) // system + user + assistant
        assertThat(result.messages[0].role).isEqualTo(MessageRole.SYSTEM)
        assertThat(result.messages[0].content).isEqualTo("You are a helpful assistant.")
        assertThat(result.messages[1].role).isEqualTo(MessageRole.USER)
        assertThat(result.messages[1].content).isEqualTo("Hello, how are you?")
        assertThat(result.messages[2].role).isEqualTo(MessageRole.ASSISTANT)
        assertThat(result.messages[2].content).isEqualTo("I'm doing well, thank you!")
    }

    @Test
    fun `normalizeRequest should handle request without system message`() {
        // Given
        val anthropicRequest = AnthropicRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(role = "user", content = "Hello"),
            ),
        )

        // When
        val result = normalizer.normalizeRequest(anthropicRequest)

        // Then
        assertThat(result.messages).hasSize(1)
        assertThat(result.messages[0].role).isEqualTo(MessageRole.USER)
        assertThat(result.messages[0].content).isEqualTo("Hello")
    }

    @Test
    fun `normalizeRequest should throw exception for unknown message role`() {
        // Given
        val anthropicRequest = AnthropicRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(role = "unknown", content = "Hello"),
            ),
        )

        // When & Then
        assertThatThrownBy { normalizer.normalizeRequest(anthropicRequest) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown Anthropic message role: unknown")
    }

    @Test
    fun `normalizeResponse should convert AnthropicResponse to IntermittentResponse`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            content = listOf(
                AnthropicContent(type = "text", text = "Hello! How can I help you today?"),
            ),
            usage = AnthropicUsage(inputTokens = 10, outputTokens = 8),
        )

        // When
        val result = normalizer.normalizeResponse(anthropicResponse)

        // Then
        assertThat(result.id).isEqualTo("msg_123")
        assertThat(result.model).isEqualTo("claude-3-sonnet-20240229")
        assertThat(result.choices).hasSize(1)
        assertThat(result.choices[0].index).isEqualTo(0)
        assertThat(result.choices[0].message?.role).isEqualTo(MessageRole.ASSISTANT)
        assertThat(result.choices[0].message?.content).isEqualTo("Hello! How can I help you today?")
        assertThat(result.usage?.promptTokens).isEqualTo(10)
        assertThat(result.usage?.completionTokens).isEqualTo(8)
        assertThat(result.usage?.totalTokens).isEqualTo(18)
    }

    @Test
    fun `normalizeResponse should handle multiple content blocks`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            content = listOf(
                AnthropicContent(type = "text", text = "Hello! "),
                AnthropicContent(type = "text", text = "How can I help you?"),
            ),
        )

        // When
        val result = normalizer.normalizeResponse(anthropicResponse)

        // Then
        assertThat(result.choices[0].message?.content).isEqualTo("Hello! How can I help you?")
    }

    @Test
    fun `normalizeResponse should filter out non-text content`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            content = listOf(
                AnthropicContent(type = "text", text = "Hello!"),
                AnthropicContent(type = "image", text = null),
                AnthropicContent(type = "text", text = " How are you?"),
            ),
        )

        // When
        val result = normalizer.normalizeResponse(anthropicResponse)

        // Then
        assertThat(result.choices[0].message?.content).isEqualTo("Hello! How are you?")
    }

    @Test
    fun `normalizeResponse should handle response without usage`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            content = listOf(
                AnthropicContent(type = "text", text = "Hello!"),
            ),
        )

        // When
        val result = normalizer.normalizeResponse(anthropicResponse)

        // Then
        assertThat(result.usage).isNull()
    }

    @Test
    fun `normalizeStreamingResponse should convert AnthropicStreamEvent to IntermittentStreamEvent`() {
        TestUtils.runTest<Unit> {
            // Given
            val streamEvents = flowOf(
                AnthropicStreamEvent(
                    type = "message_start",
                    id = "msg_123",
                    model = "claude-3-sonnet-20240229",
                ),
                AnthropicStreamEvent(
                    type = "content_block_delta",
                    delta = AnthropicDelta(type = "text_delta", text = "Hello"),
                ),
                AnthropicStreamEvent(
                    type = "content_block_delta",
                    delta = AnthropicDelta(type = "text_delta", text = " there!"),
                ),
                AnthropicStreamEvent(
                    type = "message_delta",
                    usage = AnthropicUsage(inputTokens = 10, outputTokens = 2),
                ),
            )

            // When
            val result = TestUtils.collectAll(normalizer.normalizeStreamingResponse(streamEvents))

            // Then
            assertThat(result).hasSize(4)

            val messageStart = result[0] as com.fatihcure.kolo.core.IntermittentStreamEvent.MessageStart
            assertThat(messageStart.id).isEqualTo("msg_123")
            assertThat(messageStart.model).isEqualTo("claude-3-sonnet-20240229")

            val delta1 = result[1] as com.fatihcure.kolo.core.IntermittentStreamEvent.MessageDelta
            assertThat(delta1.delta?.content).isEqualTo("Hello")

            val delta2 = result[2] as com.fatihcure.kolo.core.IntermittentStreamEvent.MessageDelta
            assertThat(delta2.delta?.content).isEqualTo(" there!")

            val messageEnd = result[3] as com.fatihcure.kolo.core.IntermittentStreamEvent.MessageEnd
            assertThat(messageEnd.usage?.promptTokens).isEqualTo(10)
            assertThat(messageEnd.usage?.completionTokens).isEqualTo(2)
            assertThat(messageEnd.usage?.totalTokens).isEqualTo(12)
        }
    }

    @Test
    fun `normalizeStreamingResponse should handle error events`() {
        TestUtils.runTest<Unit> {
            // Given
            val streamEvents = flowOf(
                AnthropicStreamEvent(
                    type = "error",
                    error = AnthropicError(type = "invalid_request", message = "Invalid request"),
                ),
            )

            // When
            val result = TestUtils.collectAll(normalizer.normalizeStreamingResponse(streamEvents))

            // Then
            assertThat(result).hasSize(1)
            val error = result[0] as com.fatihcure.kolo.core.IntermittentStreamEvent.Error
            assertThat(error.error.type).isEqualTo("invalid_request")
            assertThat(error.error.message).isEqualTo("Invalid request")
        }
    }

    @Test
    fun `normalizeStreamingResponse should throw exception for unknown event type`() {
        TestUtils.runTest<Unit> {
            // Given
            val streamEvents = flowOf(
                AnthropicStreamEvent(type = "unknown_event"),
            )

            // When & Then
            assertThatThrownBy {
                TestUtils.runTest {
                    TestUtils.collectAll(normalizer.normalizeStreamingResponse(streamEvents))
                }
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Unknown Anthropic stream event type: unknown_event")
        }
    }

    @Test
    fun `normalizeError should convert AnthropicError to IntermittentError`() {
        // Given
        val anthropicError = AnthropicError(
            type = "invalid_request",
            message = "The request is invalid",
        )

        // When
        val result = normalizer.normalizeError(anthropicError)

        // Then
        assertThat(result.type).isEqualTo("invalid_request")
        assertThat(result.message).isEqualTo("The request is invalid")
    }

    @Test
    fun `normalizeUsage should convert AnthropicUsage to IntermittentUsage`() {
        // Given
        val anthropicUsage = AnthropicUsage(inputTokens = 15, outputTokens = 25)

        // When
        val result = normalizer.normalizeUsage(anthropicUsage)

        // Then
        assertThat(result.promptTokens).isEqualTo(15)
        assertThat(result.completionTokens).isEqualTo(25)
        assertThat(result.totalTokens).isEqualTo(40)
    }
}
