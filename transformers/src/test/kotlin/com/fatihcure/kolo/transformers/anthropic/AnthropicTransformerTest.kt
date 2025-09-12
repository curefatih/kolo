package com.fatihcure.kolo.transformers.anthropic

import com.fatihcure.kolo.core.IntermittentChoice
import com.fatihcure.kolo.core.IntermittentDelta
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentMessage
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.IntermittentUsage
import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.normalizers.anthropic.AnthropicContent
import com.fatihcure.kolo.normalizers.anthropic.AnthropicDelta
import com.fatihcure.kolo.normalizers.anthropic.AnthropicError
import com.fatihcure.kolo.normalizers.anthropic.AnthropicMessage
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent
import com.fatihcure.kolo.normalizers.anthropic.AnthropicUsage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AnthropicTransformerTest {

    private lateinit var transformer: AnthropicTransformer

    @BeforeEach
    fun setUp() {
        transformer = AnthropicTransformer()
    }

    @Test
    fun `should transform request with system message`() {
        // Given
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.SYSTEM, content = "You are a helpful assistant"),
                IntermittentMessage(role = MessageRole.USER, content = "Hello, world!"),
            ),
            model = "claude-3-sonnet-20240229",
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            stop = listOf("STOP"),
            stream = false,
        )

        // When
        val result = transformer.transformRequest(request)

        // Then
        assertThat(result).isEqualTo(
            AnthropicRequest(
                model = "claude-3-sonnet-20240229",
                messages = listOf(
                    AnthropicMessage(role = "user", content = "Hello, world!"),
                ),
                system = "You are a helpful assistant",
                temperature = 0.7,
                maxTokens = 100,
                topP = 0.9,
                stop = listOf("STOP"),
                stream = false,
            ),
        )
    }

    @Test
    fun `should transform request without system message`() {
        // Given
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.USER, content = "Hello, world!"),
                IntermittentMessage(role = MessageRole.ASSISTANT, content = "Hi there!"),
            ),
            model = "claude-3-sonnet-20240229",
            temperature = 0.7,
        )

        // When
        val result = transformer.transformRequest(request)

        // Then
        assertThat(result).isEqualTo(
            AnthropicRequest(
                model = "claude-3-sonnet-20240229",
                messages = listOf(
                    AnthropicMessage(role = "user", content = "Hello, world!"),
                    AnthropicMessage(role = "assistant", content = "Hi there!"),
                ),
                system = null,
                temperature = 0.7,
                maxTokens = null,
                topP = null,
                stop = null,
                stream = false,
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
            model = "claude-3-sonnet-20240229",
        )

        // When
        val result = transformer.transformRequest(request)

        // Then
        assertThat(result).isEqualTo(
            AnthropicRequest(
                model = "claude-3-sonnet-20240229",
                messages = listOf(
                    AnthropicMessage(role = "user", content = "Hello"),
                ),
                system = null,
                temperature = null,
                maxTokens = null,
                topP = null,
                stop = null,
                stream = false,
            ),
        )
    }

    @Test
    fun `should throw exception for unsupported message role`() {
        // Given
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(role = MessageRole.TOOL, content = "Tool message"),
            ),
            model = "claude-3-sonnet-20240229",
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            transformer.transformRequest(request)
        }
    }

    @Test
    fun `should transform response with usage`() {
        // Given
        val response = IntermittentResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Hello! How can I help you today?",
                    ),
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
            AnthropicResponse(
                id = "msg_123",
                model = "claude-3-sonnet-20240229",
                content = listOf(
                    AnthropicContent(
                        type = "text",
                        text = "Hello! How can I help you today?",
                    ),
                ),
                usage = AnthropicUsage(
                    inputTokens = 10,
                    outputTokens = 15,
                ),
            ),
        )
    }

    @Test
    fun `should transform response without usage`() {
        // Given
        val response = IntermittentResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
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
            AnthropicResponse(
                id = "msg_123",
                model = "claude-3-sonnet-20240229",
                content = listOf(
                    AnthropicContent(
                        type = "text",
                        text = "Hello!",
                    ),
                ),
                usage = null,
            ),
        )
    }

    @Test
    fun `should transform response with empty assistant message`() {
        // Given
        val response = IntermittentResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.USER,
                        content = "Hello",
                    ),
                ),
            ),
        )

        // When
        val result = transformer.transformResponse(response)

        // Then
        assertThat(result).isEqualTo(
            AnthropicResponse(
                id = "msg_123",
                model = "claude-3-sonnet-20240229",
                content = listOf(
                    AnthropicContent(
                        type = "text",
                        text = "",
                    ),
                ),
                usage = null,
            ),
        )
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
            AnthropicError(
                type = "invalid_request_error",
                message = "Invalid request parameters",
            ),
        )
    }

    @Test
    fun `should transform streaming response`() = runBlocking {
        // Given
        val streamEvents = listOf(
            IntermittentStreamEvent.MessageStart(
                id = "msg_123",
                model = "claude-3-sonnet-20240229",
            ),
            IntermittentStreamEvent.MessageDelta(
                delta = IntermittentDelta(content = "Hello"),
            ),
            IntermittentStreamEvent.MessageDelta(
                delta = IntermittentDelta(content = " world!"),
            ),
            IntermittentStreamEvent.MessageEnd(
                finishReason = "end_turn",
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
            AnthropicStreamEvent(
                type = "message_start",
                id = "msg_123",
                model = "claude-3-sonnet-20240229",
            ),
        )
        assertThat(result[1]).isEqualTo(
            AnthropicStreamEvent(
                type = "content_block_delta",
                delta = AnthropicDelta(
                    type = "text_delta",
                    text = "Hello",
                ),
            ),
        )
        assertThat(result[2]).isEqualTo(
            AnthropicStreamEvent(
                type = "content_block_delta",
                delta = AnthropicDelta(
                    type = "text_delta",
                    text = " world!",
                ),
            ),
        )
        assertThat(result[3]).isEqualTo(
            AnthropicStreamEvent(
                type = "message_delta",
                usage = AnthropicUsage(
                    inputTokens = 10,
                    outputTokens = 2,
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
            AnthropicStreamEvent(
                type = "error",
                error = AnthropicError(
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
}

private fun <T> List<T>.asFlow() = kotlinx.coroutines.flow.flow {
    this@asFlow.forEach { emit(it) }
}
