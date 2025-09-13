package com.fatihcure.kolo.normalizers.openai

import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.normalizers.TestUtils
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenAINormalizerTest {

    private lateinit var normalizer: OpenAINormalizer

    @BeforeEach
    fun setUp() {
        normalizer = OpenAINormalizer()
    }

    @Test
    fun `normalizeRequest should convert OpenAIRequest to IntermittentRequest`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                OpenAIMessage(role = "system", content = "You are a helpful assistant."),
                OpenAIMessage(role = "user", content = "Hello, how are you?"),
                OpenAIMessage(role = "assistant", content = "I'm doing well, thank you!"),
            ),
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            frequencyPenalty = 0.1,
            presencePenalty = 0.1,
            stop = listOf("STOP"),
            stream = false,
        )

        // When
        val result = normalizer.normalizeRequest(openAIRequest)

        // Then
        assertThat(result.model).isEqualTo("gpt-4")
        assertThat(result.temperature).isEqualTo(0.7)
        assertThat(result.maxTokens).isEqualTo(100)
        assertThat(result.topP).isEqualTo(0.9)
        assertThat(result.frequencyPenalty).isEqualTo(0.1)
        assertThat(result.presencePenalty).isEqualTo(0.1)
        assertThat(result.stop).isEqualTo(listOf("STOP"))
        assertThat(result.stream).isFalse()

        // Check messages
        assertThat(result.messages).hasSize(3)
        assertThat(result.messages[0].role).isEqualTo(MessageRole.SYSTEM)
        assertThat(result.messages[0].content).isEqualTo("You are a helpful assistant.")
        assertThat(result.messages[1].role).isEqualTo(MessageRole.USER)
        assertThat(result.messages[1].content).isEqualTo("Hello, how are you?")
        assertThat(result.messages[2].role).isEqualTo(MessageRole.ASSISTANT)
        assertThat(result.messages[2].content).isEqualTo("I'm doing well, thank you!")
    }

    @Test
    fun `normalizeRequest should handle messages with names`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Hello", name = "John"),
            ),
        )

        // When
        val result = normalizer.normalizeRequest(openAIRequest)

        // Then
        assertThat(result.messages).hasSize(1)
        assertThat(result.messages[0].role).isEqualTo(MessageRole.USER)
        assertThat(result.messages[0].content).isEqualTo("Hello")
        assertThat(result.messages[0].name).isEqualTo("John")
    }

    @Test
    fun `normalizeRequest should handle tool messages`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                OpenAIMessage(role = "tool", content = "Tool response", name = "calculator"),
            ),
        )

        // When
        val result = normalizer.normalizeRequest(openAIRequest)

        // Then
        assertThat(result.messages).hasSize(1)
        assertThat(result.messages[0].role).isEqualTo(MessageRole.TOOL)
        assertThat(result.messages[0].content).isEqualTo("Tool response")
        assertThat(result.messages[0].name).isEqualTo("calculator")
    }

    @Test
    fun `normalizeRequest should throw exception for unknown message role`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                OpenAIMessage(role = "unknown", content = "Hello"),
            ),
        )

        // When & Then
        assertThatThrownBy { normalizer.normalizeRequest(openAIRequest) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown OpenAI message role: unknown")
    }

    @Test
    fun `normalizeResponse should convert OpenAIResponse to IntermittentResponse`() {
        // Given
        val openAIResponse = OpenAIResponse(
            id = "chatcmpl-123",
            model = "gpt-4",
            choices = listOf(
                OpenAIChoice(
                    index = 0,
                    message = OpenAIMessage(role = "assistant", content = "Hello! How can I help you?"),
                    finishReason = "stop",
                ),
            ),
            usage = OpenAIUsage(promptTokens = 10, completionTokens = 8, totalTokens = 18),
        )

        // When
        val result = normalizer.normalizeResponse(openAIResponse)

        // Then
        assertThat(result.id).isEqualTo("chatcmpl-123")
        assertThat(result.model).isEqualTo("gpt-4")
        assertThat(result.choices).hasSize(1)
        assertThat(result.choices[0].index).isEqualTo(0)
        assertThat(result.choices[0].message?.role).isEqualTo(MessageRole.ASSISTANT)
        assertThat(result.choices[0].message?.content).isEqualTo("Hello! How can I help you?")
        assertThat(result.choices[0].finishReason).isEqualTo("stop")
        assertThat(result.usage?.promptTokens).isEqualTo(10)
        assertThat(result.usage?.completionTokens).isEqualTo(8)
        assertThat(result.usage?.totalTokens).isEqualTo(18)
    }

    @Test
    fun `normalizeResponse should handle multiple choices`() {
        // Given
        val openAIResponse = OpenAIResponse(
            id = "chatcmpl-123",
            model = "gpt-4",
            choices = listOf(
                OpenAIChoice(
                    index = 0,
                    message = OpenAIMessage(role = "assistant", content = "First choice"),
                ),
                OpenAIChoice(
                    index = 1,
                    message = OpenAIMessage(role = "assistant", content = "Second choice"),
                ),
            ),
        )

        // When
        val result = normalizer.normalizeResponse(openAIResponse)

        // Then
        assertThat(result.choices).hasSize(2)
        assertThat(result.choices[0].index).isEqualTo(0)
        assertThat(result.choices[0].message?.content).isEqualTo("First choice")
        assertThat(result.choices[1].index).isEqualTo(1)
        assertThat(result.choices[1].message?.content).isEqualTo("Second choice")
    }

    @Test
    fun `normalizeResponse should handle response without usage`() {
        // Given
        val openAIResponse = OpenAIResponse(
            id = "chatcmpl-123",
            model = "gpt-4",
            choices = listOf(
                OpenAIChoice(
                    index = 0,
                    message = OpenAIMessage(role = "assistant", content = "Hello!"),
                ),
            ),
        )

        // When
        val result = normalizer.normalizeResponse(openAIResponse)

        // Then
        assertThat(result.usage).isNull()
    }

    @Test
    fun `normalizeStreamingResponse should convert OpenAIStreamingResponse to IntermittentStreamEvent`() {
        TestUtils.runTest<Unit> {
            // Given
            val streamEvents = flowOf(
                OpenAIStreamingResponse(
                    id = "chatcmpl-123",
                    model = "gpt-4",
                    choices = listOf(
                        OpenAIStreamingChoice(
                            index = 0,
                            delta = OpenAIStreamingDelta(role = "assistant", content = "Hello"),
                        ),
                    ),
                ),
                OpenAIStreamingResponse(
                    choices = listOf(
                        OpenAIStreamingChoice(
                            index = 0,
                            delta = OpenAIStreamingDelta(content = " there!"),
                        ),
                    ),
                ),
                OpenAIStreamingResponse(
                    choices = listOf(
                        OpenAIStreamingChoice(
                            index = 0,
                            finishReason = "stop",
                        ),
                    ),
                    usage = OpenAIUsage(promptTokens = 10, completionTokens = 2, totalTokens = 12),
                ),
            )

            // When
            val result = TestUtils.collectAll(normalizer.normalizeStreamingResponse(streamEvents))

            // Then
            assertThat(result).hasSize(3)

            val messageStart = result[0] as com.fatihcure.kolo.core.IntermittentStreamEvent.MessageStart
            assertThat(messageStart.id).isEqualTo("chatcmpl-123")
            assertThat(messageStart.model).isEqualTo("gpt-4")

            val delta = result[1] as com.fatihcure.kolo.core.IntermittentStreamEvent.MessageDelta
            assertThat(delta.delta?.content).isEqualTo(" there!")

            val messageEnd = result[2] as com.fatihcure.kolo.core.IntermittentStreamEvent.MessageEnd
            assertThat(messageEnd.finishReason).isEqualTo("stop")
            assertThat(messageEnd.usage?.promptTokens).isEqualTo(10)
            assertThat(messageEnd.usage?.completionTokens).isEqualTo(2)
            assertThat(messageEnd.usage?.totalTokens).isEqualTo(12)
        }
    }

    @Test
    fun `normalizeStreamingResponse should handle error events`() = TestUtils.runTest<Unit>
        {
            // Given
            val streamEvents = flowOf(
                OpenAIStreamingResponse(
                    error = OpenAIError(
                        type = "invalid_request_error",
                        message = "Invalid request",
                        code = "invalid_request",
                        param = "model",
                    ),
                ),
            )

            // When
            val result = TestUtils.collectAll(normalizer.normalizeStreamingResponse(streamEvents))

            // Then
            assertThat(result).hasSize(1)
            val error = result[0] as com.fatihcure.kolo.core.IntermittentStreamEvent.Error
            assertThat(error.error.type).isEqualTo("invalid_request_error")
            assertThat(error.error.message).isEqualTo("Invalid request")
            assertThat(error.error.code).isEqualTo("invalid_request")
            assertThat(error.error.param).isEqualTo("model")
        }

    @Test
    fun `normalizeStreamingResponse should throw exception for unknown event type`() = TestUtils.runTest<Unit> {
        // Given
        val streamEvents = flowOf(
            OpenAIStreamingResponse(
                choices = listOf(
                    OpenAIStreamingChoice(index = 0), // No message, delta, or finishReason
                ),
            ),
        )

        // When & Then
        assertThatThrownBy {
            TestUtils.runTest {
                TestUtils.collectAll(normalizer.normalizeStreamingResponse(streamEvents))
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown OpenAI streaming response type")
    }

    @Test
    fun `normalizeError should convert OpenAIError to IntermittentError`() {
        // Given
        val openAIError = OpenAIError(
            type = "invalid_request_error",
            message = "The request is invalid",
            code = "invalid_request",
            param = "model",
        )

        // When
        val result = normalizer.normalizeError(openAIError)

        // Then
        assertThat(result.type).isEqualTo("invalid_request_error")
        assertThat(result.message).isEqualTo("The request is invalid")
        assertThat(result.code).isEqualTo("invalid_request")
        assertThat(result.param).isEqualTo("model")
    }

    @Test
    fun `normalizeError should handle error without optional fields`() {
        // Given
        val openAIError = OpenAIError(
            type = "server_error",
            message = "Internal server error",
        )

        // When
        val result = normalizer.normalizeError(openAIError)

        // Then
        assertThat(result.type).isEqualTo("server_error")
        assertThat(result.message).isEqualTo("Internal server error")
        assertThat(result.code).isNull()
        assertThat(result.param).isNull()
    }

    @Test
    fun `normalizeMessage should convert OpenAIMessage to IntermittentMessage`() {
        // Given
        val openAIMessage = OpenAIMessage(
            role = "user",
            content = "Hello",
            name = "John",
        )

        // When
        val result = normalizer.normalizeMessage(openAIMessage)

        // Then
        assertThat(result.role).isEqualTo(MessageRole.USER)
        assertThat(result.content).isEqualTo("Hello")
        assertThat(result.name).isEqualTo("John")
    }

    @Test
    fun `normalizeChoice should convert OpenAIChoice to IntermittentChoice`() {
        // Given
        val openAIChoice = OpenAIChoice(
            index = 0,
            message = OpenAIMessage(role = "assistant", content = "Hello"),
            delta = OpenAIDelta(content = " there"),
            finishReason = "stop",
        )

        // When
        val result = normalizer.normalizeChoice(openAIChoice)

        // Then
        assertThat(result.index).isEqualTo(0)
        assertThat(result.message?.role).isEqualTo(MessageRole.ASSISTANT)
        assertThat(result.message?.content).isEqualTo("Hello")
        assertThat(result.delta?.content).isEqualTo(" there")
        assertThat(result.finishReason).isEqualTo("stop")
    }

    @Test
    fun `normalizeDelta should convert OpenAIDelta to IntermittentDelta`() {
        // Given
        val openAIDelta = OpenAIDelta(
            role = "assistant",
            content = "Hello",
            name = "assistant",
        )

        // When
        val result = normalizer.normalizeDelta(openAIDelta)

        // Then
        assertThat(result.role).isEqualTo(MessageRole.ASSISTANT)
        assertThat(result.content).isEqualTo("Hello")
        assertThat(result.name).isEqualTo("assistant")
    }

    @Test
    fun `normalizeDelta should throw exception for unknown delta role`() {
        // Given
        val openAIDelta = OpenAIDelta(
            role = "unknown",
            content = "Hello",
        )

        // When & Then
        assertThatThrownBy { normalizer.normalizeDelta(openAIDelta) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown OpenAI delta role: unknown")
    }

    @Test
    fun `normalizeUsage should convert OpenAIUsage to IntermittentUsage`() {
        // Given
        val openAIUsage = OpenAIUsage(
            promptTokens = 15,
            completionTokens = 25,
            totalTokens = 40,
        )

        // When
        val result = normalizer.normalizeUsage(openAIUsage)

        // Then
        assertThat(result.promptTokens).isEqualTo(15)
        assertThat(result.completionTokens).isEqualTo(25)
        assertThat(result.totalTokens).isEqualTo(40)
    }
}
