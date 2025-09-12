package com.fatihcure.kolo.transformers.openai

import com.fatihcure.kolo.core.IntermittentChoice
import com.fatihcure.kolo.core.IntermittentDelta
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentMessage
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.IntermittentUsage
import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.normalizers.openai.OpenAIChoice
import com.fatihcure.kolo.normalizers.openai.OpenAIDelta
import com.fatihcure.kolo.normalizers.openai.OpenAIError
import com.fatihcure.kolo.normalizers.openai.OpenAIMessage
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent
import com.fatihcure.kolo.normalizers.openai.OpenAIUsage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenAITransformerTest {

    private lateinit var transformer: OpenAITransformer

    @BeforeEach
    fun setUp() {
        transformer = OpenAITransformer()
    }

    @Test
    fun `should transform request with all fields`() {
        // Given
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.SYSTEM, content = "You are a helpful assistant"),
                IntermittentMessage(role = MessageRole.USER, content = "Hello, world!", name = "user1"),
                IntermittentMessage(role = MessageRole.ASSISTANT, content = "Hi there!"),
                IntermittentMessage(role = MessageRole.TOOL, content = "Tool response", name = "tool1"),
            ),
            model = "gpt-4",
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            frequencyPenalty = 0.1,
            presencePenalty = 0.2,
            stop = listOf("STOP", "END"),
            stream = true,
        )

        // When
        val result = transformer.transformRequest(request)

        // Then
        assertThat(result).isEqualTo(
            OpenAIRequest(
                model = "gpt-4",
                messages = listOf(
                    OpenAIMessage(role = "system", content = "You are a helpful assistant"),
                    OpenAIMessage(role = "user", content = "Hello, world!", name = "user1"),
                    OpenAIMessage(role = "assistant", content = "Hi there!"),
                    OpenAIMessage(role = "tool", content = "Tool response", name = "tool1"),
                ),
                temperature = 0.7,
                maxTokens = 100,
                topP = 0.9,
                frequencyPenalty = 0.1,
                presencePenalty = 0.2,
                stop = listOf("STOP", "END"),
                stream = true,
            ),
        )
    }

    @Test
    fun `should transform request with minimal fields`() {
        // Given
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.USER, content = "Hello"),
            ),
            model = "gpt-3.5-turbo",
        )

        // When
        val result = transformer.transformRequest(request)

        // Then
        assertThat(result).isEqualTo(
            OpenAIRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    OpenAIMessage(role = "user", content = "Hello"),
                ),
                temperature = null,
                maxTokens = null,
                topP = null,
                frequencyPenalty = null,
                presencePenalty = null,
                stop = null,
                stream = false,
            ),
        )
    }

    @Test
    fun `should transform request without optional parameters`() {
        // Given
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.USER, content = "Hello"),
                IntermittentMessage(role = MessageRole.ASSISTANT, content = "Hi!"),
            ),
            model = "gpt-4",
            temperature = 0.5,
            maxTokens = 50,
        )

        // When
        val result = transformer.transformRequest(request)

        // Then
        assertThat(result).isEqualTo(
            OpenAIRequest(
                model = "gpt-4",
                messages = listOf(
                    OpenAIMessage(role = "user", content = "Hello"),
                    OpenAIMessage(role = "assistant", content = "Hi!"),
                ),
                temperature = 0.5,
                maxTokens = 50,
                topP = null,
                frequencyPenalty = null,
                presencePenalty = null,
                stop = null,
                stream = false,
            ),
        )
    }

    @Test
    fun `should transform response with usage`() {
        // Given
        val response = IntermittentResponse(
            id = "chatcmpl-123",
            model = "gpt-4",
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Hello! How can I help you today?",
                    ),
                    finishReason = "stop",
                ),
            ),
            usage = IntermittentUsage(
                promptTokens = 10,
                completionTokens = 15,
                totalTokens = 25,
            ),
        )

        // When
        val result = transformer.transformResponse(response)

        // Then
        assertThat(result).isEqualTo(
            OpenAIResponse(
                id = "chatcmpl-123",
                model = "gpt-4",
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        message = OpenAIMessage(
                            role = "assistant",
                            content = "Hello! How can I help you today?",
                        ),
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAIUsage(
                    promptTokens = 10,
                    completionTokens = 15,
                    totalTokens = 25,
                ),
            ),
        )
    }

    @Test
    fun `should transform response without usage`() {
        // Given
        val response = IntermittentResponse(
            id = "chatcmpl-123",
            model = "gpt-4",
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Hello!",
                    ),
                ),
            ),
        )

        // When
        val result = transformer.transformResponse(response)

        // Then
        assertThat(result).isEqualTo(
            OpenAIResponse(
                id = "chatcmpl-123",
                model = "gpt-4",
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        message = OpenAIMessage(
                            role = "assistant",
                            content = "Hello!",
                        ),
                    ),
                ),
                usage = null,
            ),
        )
    }

    @Test
    fun `should transform response with multiple choices`() {
        // Given
        val response = IntermittentResponse(
            id = "chatcmpl-123",
            model = "gpt-4",
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "First choice",
                    ),
                    finishReason = "stop",
                ),
                IntermittentChoice(
                    index = 1,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Second choice",
                    ),
                    finishReason = "length",
                ),
            ),
        )

        // When
        val result = transformer.transformResponse(response)

        // Then
        assertThat(result.choices).hasSize(2)
        assertThat(result.choices[0].index).isEqualTo(0)
        assertThat(result.choices[0].message?.content).isEqualTo("First choice")
        assertThat(result.choices[0].finishReason).isEqualTo("stop")
        assertThat(result.choices[1].index).isEqualTo(1)
        assertThat(result.choices[1].message?.content).isEqualTo("Second choice")
        assertThat(result.choices[1].finishReason).isEqualTo("length")
    }

    @Test
    fun `should transform error`() {
        // Given
        val error = IntermittentError(
            type = "invalid_request_error",
            message = "Invalid request parameters",
            code = "invalid_parameter",
            param = "temperature",
        )

        // When
        val result = transformer.transformError(error)

        // Then
        assertThat(result).isEqualTo(
            OpenAIError(
                type = "invalid_request_error",
                message = "Invalid request parameters",
                code = "invalid_parameter",
                param = "temperature",
            ),
        )
    }

    @Test
    fun `should transform error without optional fields`() {
        // Given
        val error = IntermittentError(
            type = "rate_limit_error",
            message = "Rate limit exceeded",
        )

        // When
        val result = transformer.transformError(error)

        // Then
        assertThat(result).isEqualTo(
            OpenAIError(
                type = "rate_limit_error",
                message = "Rate limit exceeded",
                code = null,
                param = null,
            ),
        )
    }

    @Test
    fun `should transform streaming response`() = runBlocking {
        // Given
        val streamEvents = listOf(
            IntermittentStreamEvent.MessageStart(
                id = "chatcmpl-123",
                model = "gpt-4",
            ),
            IntermittentStreamEvent.MessageDelta(
                delta = IntermittentDelta(content = "Hello"),
            ),
            IntermittentStreamEvent.MessageDelta(
                delta = IntermittentDelta(content = " world!"),
            ),
            IntermittentStreamEvent.MessageEnd(
                finishReason = "stop",
                usage = IntermittentUsage(
                    promptTokens = 10,
                    completionTokens = 2,
                    totalTokens = 12,
                ),
            ),
        )

        // When
        val result = transformer.transformStreamingResponse(streamEvents.asFlow()).toList()

        // Then
        assertThat(result).hasSize(4)
        assertThat(result[0]).isEqualTo(
            OpenAIStreamEvent(
                id = "chatcmpl-123",
                model = "gpt-4",
            ),
        )
        assertThat(result[1]).isEqualTo(
            OpenAIStreamEvent(
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        delta = OpenAIDelta(content = "Hello"),
                    ),
                ),
            ),
        )
        assertThat(result[2]).isEqualTo(
            OpenAIStreamEvent(
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        delta = OpenAIDelta(content = " world!"),
                    ),
                ),
            ),
        )
        assertThat(result[3]).isEqualTo(
            OpenAIStreamEvent(
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAIUsage(
                    promptTokens = 10,
                    completionTokens = 2,
                    totalTokens = 12,
                ),
            ),
        )
    }

    @Test
    fun `should transform streaming response with role delta`() = runBlocking {
        // Given
        val streamEvents = listOf(
            IntermittentStreamEvent.MessageDelta(
                delta = IntermittentDelta(
                    role = MessageRole.ASSISTANT,
                    content = "Hello",
                    name = "assistant",
                ),
            ),
        )

        // When
        val result = transformer.transformStreamingResponse(streamEvents.asFlow()).toList()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(
            OpenAIStreamEvent(
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        delta = OpenAIDelta(
                            role = "assistant",
                            content = "Hello",
                            name = "assistant",
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `should transform streaming error`() = runBlocking {
        // Given
        val error = IntermittentError(
            type = "rate_limit_error",
            message = "Rate limit exceeded",
        )
        val streamEvents = listOf(
            IntermittentStreamEvent.Error(error = error),
        )

        // When
        val result = transformer.transformStreamingResponse(streamEvents.asFlow()).toList()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(
            OpenAIStreamEvent(
                error = OpenAIError(
                    type = "rate_limit_error",
                    message = "Rate limit exceeded",
                ),
            ),
        )
    }

    @Test
    fun `should handle empty streaming response`() = runBlocking {
        // Given
        val streamEvents = emptyList<IntermittentStreamEvent>()

        // When
        val result = transformer.transformStreamingResponse(streamEvents.asFlow()).toList()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `should transform message with all roles`() {
        // Given
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.SYSTEM, content = "System message"),
                IntermittentMessage(role = MessageRole.USER, content = "User message", name = "user1"),
                IntermittentMessage(role = MessageRole.ASSISTANT, content = "Assistant message"),
                IntermittentMessage(role = MessageRole.TOOL, content = "Tool message", name = "tool1"),
            ),
            model = "gpt-4",
        )

        // When
        val result = transformer.transformRequest(request)

        // Then
        assertThat(result.messages).hasSize(4)
        assertThat(result.messages[0].role).isEqualTo("system")
        assertThat(result.messages[0].content).isEqualTo("System message")
        assertThat(result.messages[0].name).isNull()

        assertThat(result.messages[1].role).isEqualTo("user")
        assertThat(result.messages[1].content).isEqualTo("User message")
        assertThat(result.messages[1].name).isEqualTo("user1")

        assertThat(result.messages[2].role).isEqualTo("assistant")
        assertThat(result.messages[2].content).isEqualTo("Assistant message")
        assertThat(result.messages[2].name).isNull()

        assertThat(result.messages[3].role).isEqualTo("tool")
        assertThat(result.messages[3].content).isEqualTo("Tool message")
        assertThat(result.messages[3].name).isEqualTo("tool1")
    }
}

private fun <T> List<T>.asFlow() = kotlinx.coroutines.flow.flow {
    this@asFlow.forEach { emit(it) }
}
