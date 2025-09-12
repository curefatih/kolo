package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.normalizers.anthropic.AnthropicContent
import com.fatihcure.kolo.normalizers.anthropic.AnthropicMessage
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.anthropic.AnthropicUsage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AnthropicProviderTest {

    private lateinit var anthropicProvider: AnthropicProvider

    @BeforeEach
    fun setUp() {
        anthropicProvider = AnthropicProvider()
    }

    @Test
    fun `should normalize Anthropic request to intermittent request`() {
        // Given
        val anthropicRequest = AnthropicRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(role = "user", content = "Hello, world!"),
            ),
            system = "You are a helpful assistant",
            temperature = 0.7,
            maxTokens = 100,
        )

        // When
        val intermittentRequest = anthropicProvider.normalizeRequest(anthropicRequest)

        // Then
        assertThat(intermittentRequest).isNotNull
        assertThat(intermittentRequest.model).isEqualTo("claude-3-sonnet-20240229")
        assertThat(intermittentRequest.messages).hasSize(2) // system + user message
        assertThat(intermittentRequest.messages[0].role).isEqualTo(com.fatihcure.kolo.core.MessageRole.SYSTEM)
        assertThat(intermittentRequest.messages[0].content).isEqualTo("You are a helpful assistant")
        assertThat(intermittentRequest.messages[1].role).isEqualTo(com.fatihcure.kolo.core.MessageRole.USER)
        assertThat(intermittentRequest.messages[1].content).isEqualTo("Hello, world!")
        assertThat(intermittentRequest.temperature).isEqualTo(0.7)
        assertThat(intermittentRequest.maxTokens).isEqualTo(100)
    }

    @Test
    fun `should transform intermittent request to Anthropic request`() {
        // Given
        val intermittentRequest = IntermittentRequest(
            messages = listOf(
                com.fatihcure.kolo.core.IntermittentMessage(
                    role = com.fatihcure.kolo.core.MessageRole.USER,
                    content = "Hello, world!",
                ),
            ),
            model = "claude-3-sonnet-20240229",
            temperature = 0.7,
            maxTokens = 100,
        )

        // When
        val anthropicRequest = anthropicProvider.transformRequest(intermittentRequest)

        // Then
        assertThat(anthropicRequest).isNotNull
        assertThat(anthropicRequest.model).isEqualTo("claude-3-sonnet-20240229")
        assertThat(anthropicRequest.messages).hasSize(1)
        assertThat(anthropicRequest.messages[0].content).isEqualTo("Hello, world!")
        assertThat(anthropicRequest.temperature).isEqualTo(0.7)
        assertThat(anthropicRequest.maxTokens).isEqualTo(100)
    }

    @Test
    fun `should normalize Anthropic response to intermittent response`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "test-id",
            model = "claude-3-sonnet-20240229",
            content = listOf(
                AnthropicContent(type = "text", text = "Hello!"),
            ),
            usage = AnthropicUsage(
                inputTokens = 10,
                outputTokens = 5,
            ),
        )

        // When
        val intermittentResponse = anthropicProvider.normalizeResponse(anthropicResponse)

        // Then
        assertThat(intermittentResponse).isNotNull
        assertThat(intermittentResponse.id).isEqualTo("test-id")
        assertThat(intermittentResponse.model).isEqualTo("claude-3-sonnet-20240229")
        assertThat(intermittentResponse.choices).hasSize(1)
        assertThat(intermittentResponse.choices[0].message?.content).isEqualTo("Hello!")
        assertThat(intermittentResponse.usage?.totalTokens).isEqualTo(15)
    }

    @Test
    fun `should transform intermittent response to Anthropic response`() {
        // Given
        val intermittentResponse = IntermittentResponse(
            id = "test-id",
            model = "claude-3-sonnet-20240229",
            choices = listOf(
                com.fatihcure.kolo.core.IntermittentChoice(
                    index = 0,
                    message = com.fatihcure.kolo.core.IntermittentMessage(
                        role = com.fatihcure.kolo.core.MessageRole.ASSISTANT,
                        content = "Hello!",
                    ),
                    finishReason = "end_turn",
                ),
            ),
            usage = com.fatihcure.kolo.core.IntermittentUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15,
            ),
        )

        // When
        val anthropicResponse = anthropicProvider.transformResponse(intermittentResponse)

        // Then
        assertThat(anthropicResponse).isNotNull
        assertThat(anthropicResponse.id).isEqualTo("test-id")
        assertThat(anthropicResponse.model).isEqualTo("claude-3-sonnet-20240229")
        assertThat(anthropicResponse.content).hasSize(1)
        assertThat(anthropicResponse.content[0].text).isEqualTo("Hello!")
        assertThat(anthropicResponse.usage?.inputTokens).isEqualTo(10)
        assertThat(anthropicResponse.usage?.outputTokens).isEqualTo(5)
    }

    @Test
    fun `should throw exception when normalizing streaming response`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "test-id",
            model = "claude-3-sonnet-20240229",
            content = emptyList(),
        )
        val stream = flowOf(anthropicResponse)

        // When & Then
        assertThrows<UnsupportedOperationException> {
            runBlocking {
                anthropicProvider.normalizeStreamingResponse(stream).first()
            }
        }
    }

    @Test
    fun `should transform streaming response with empty response`() = runBlocking {
        // Given
        val streamEvent = IntermittentStreamEvent.MessageStart("test-id", "claude-3-sonnet-20240229")
        val stream = flowOf(streamEvent)

        // When
        val result = anthropicProvider.transformStreamingResponse(stream).first()

        // Then
        assertThat(result).isNotNull
        assertThat(result.id).isEmpty()
        assertThat(result.model).isEmpty()
        assertThat(result.content).isEmpty()
        assertThat(result.usage).isNull()
    }

    @Test
    fun `should normalize error from Anthropic response`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "error-id",
            model = "claude-3-sonnet-20240229",
            content = emptyList(),
        )

        // When
        val intermittentError = anthropicProvider.normalizeError(anthropicResponse)

        // Then
        assertThat(intermittentError).isNotNull
        assertThat(intermittentError.type).isEqualTo("response_error")
        assertThat(intermittentError.message).isEqualTo("Error in response processing")
    }

    @Test
    fun `should transform error to Anthropic response`() {
        // Given
        val intermittentError = IntermittentError(
            type = "test_error",
            message = "Test error message",
        )

        // When
        val anthropicResponse = anthropicProvider.transformError(intermittentError)

        // Then
        assertThat(anthropicResponse).isNotNull
        assertThat(anthropicResponse.id).isEmpty()
        assertThat(anthropicResponse.model).isEmpty()
        assertThat(anthropicResponse.content).isEmpty()
        assertThat(anthropicResponse.usage).isNull()
    }

    @Test
    fun `should handle request with all optional parameters`() {
        // Given
        val anthropicRequest = AnthropicRequest(
            model = "claude-3-opus-20240229",
            messages = listOf(
                AnthropicMessage(role = "user", content = "Hello!"),
            ),
            system = "You are a helpful assistant",
            temperature = 0.8,
            maxTokens = 200,
            topP = 0.9,
            stop = listOf("STOP", "END"),
            stream = true,
        )

        // When
        val intermittentRequest = anthropicProvider.normalizeRequest(anthropicRequest)

        // Then
        assertThat(intermittentRequest).isNotNull
        assertThat(intermittentRequest.model).isEqualTo("claude-3-opus-20240229")
        assertThat(intermittentRequest.messages).hasSize(2) // system + user message
        assertThat(intermittentRequest.messages[0].role).isEqualTo(com.fatihcure.kolo.core.MessageRole.SYSTEM)
        assertThat(intermittentRequest.messages[0].content).isEqualTo("You are a helpful assistant")
        assertThat(intermittentRequest.messages[1].role).isEqualTo(com.fatihcure.kolo.core.MessageRole.USER)
        assertThat(intermittentRequest.messages[1].content).isEqualTo("Hello!")
        assertThat(intermittentRequest.temperature).isEqualTo(0.8)
        assertThat(intermittentRequest.maxTokens).isEqualTo(200)
        assertThat(intermittentRequest.topP).isEqualTo(0.9)
        assertThat(intermittentRequest.stop).containsExactly("STOP", "END")
        assertThat(intermittentRequest.stream).isTrue
    }

    @Test
    fun `should handle response with multiple content blocks`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "multi-content-id",
            model = "claude-3-sonnet-20240229",
            content = listOf(
                AnthropicContent(type = "text", text = "First part of the response"),
                AnthropicContent(type = "text", text = "Second part of the response"),
            ),
            usage = AnthropicUsage(
                inputTokens = 20,
                outputTokens = 10,
            ),
        )

        // When
        val intermittentResponse = anthropicProvider.normalizeResponse(anthropicResponse)

        // Then
        assertThat(intermittentResponse).isNotNull
        assertThat(intermittentResponse.choices).hasSize(1)
        // The content should be concatenated
        assertThat(intermittentResponse.choices[0].message?.content).contains("First part of the response")
        assertThat(intermittentResponse.choices[0].message?.content).contains("Second part of the response")
    }

    @Test
    fun `should handle request with system message`() {
        // Given
        val anthropicRequest = AnthropicRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(role = "user", content = "Hello!"),
            ),
            system = "You are a helpful AI assistant that provides accurate information.",
        )

        // When
        val intermittentRequest = anthropicProvider.normalizeRequest(anthropicRequest)

        // Then
        assertThat(intermittentRequest).isNotNull
        assertThat(intermittentRequest.messages).hasSize(2) // system + user message
        assertThat(intermittentRequest.messages[0].role).isEqualTo(com.fatihcure.kolo.core.MessageRole.SYSTEM)
        assertThat(intermittentRequest.messages[0].content).isEqualTo("You are a helpful AI assistant that provides accurate information.")
        assertThat(intermittentRequest.messages[1].role).isEqualTo(com.fatihcure.kolo.core.MessageRole.USER)
        assertThat(intermittentRequest.messages[1].content).isEqualTo("Hello!")
    }

    @Test
    fun `should handle response without usage information`() {
        // Given
        val anthropicResponse = AnthropicResponse(
            id = "no-usage-id",
            model = "claude-3-sonnet-20240229",
            content = listOf(
                AnthropicContent(type = "text", text = "Hello!"),
            ),
            usage = null,
        )

        // When
        val intermittentResponse = anthropicProvider.normalizeResponse(anthropicResponse)

        // Then
        assertThat(intermittentResponse).isNotNull
        assertThat(intermittentResponse.usage).isNull()
    }
}
