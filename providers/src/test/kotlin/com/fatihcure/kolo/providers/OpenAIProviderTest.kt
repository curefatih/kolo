package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.normalizers.openai.OpenAIChoice
import com.fatihcure.kolo.normalizers.openai.OpenAIMessage
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIUsage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OpenAIProviderTest {

    private lateinit var openAIProvider: OpenAIProvider

    @BeforeEach
    fun setUp() {
        openAIProvider = OpenAIProvider()
    }

    @Test
    fun `should normalize OpenAI request to intermittent request`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Hello, world!"),
            ),
            temperature = 0.7,
            maxTokens = 100,
        )

        // When
        val intermittentRequest = openAIProvider.normalizeRequest(openAIRequest)

        // Then
        assertThat(intermittentRequest).isNotNull
        assertThat(intermittentRequest.model).isEqualTo("gpt-3.5-turbo")
        assertThat(intermittentRequest.messages).hasSize(1)
        assertThat(intermittentRequest.messages[0].content).isEqualTo("Hello, world!")
        assertThat(intermittentRequest.temperature).isEqualTo(0.7)
        assertThat(intermittentRequest.maxTokens).isEqualTo(100)
    }

    @Test
    fun `should transform intermittent request to OpenAI request`() {
        // Given
        val intermittentRequest = IntermittentRequest(
            messages = listOf(
                com.fatihcure.kolo.core.IntermittentMessage(
                    role = com.fatihcure.kolo.core.MessageRole.USER,
                    content = "Hello, world!",
                ),
            ),
            model = "gpt-3.5-turbo",
            temperature = 0.7,
            maxTokens = 100,
        )

        // When
        val openAIRequest = openAIProvider.transformRequest(intermittentRequest)

        // Then
        assertThat(openAIRequest).isNotNull
        assertThat(openAIRequest.model).isEqualTo("gpt-3.5-turbo")
        assertThat(openAIRequest.messages).hasSize(1)
        assertThat(openAIRequest.messages[0].content).isEqualTo("Hello, world!")
        assertThat(openAIRequest.temperature).isEqualTo(0.7)
        assertThat(openAIRequest.maxTokens).isEqualTo(100)
    }

    @Test
    fun `should normalize OpenAI response to intermittent response`() {
        // Given
        val openAIResponse = OpenAIResponse(
            id = "test-id",
            model = "gpt-3.5-turbo",
            choices = listOf(
                OpenAIChoice(
                    index = 0,
                    message = OpenAIMessage(role = "assistant", content = "Hello!"),
                    finishReason = "stop",
                ),
            ),
            usage = OpenAIUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15,
            ),
        )

        // When
        val intermittentResponse = openAIProvider.normalizeResponse(openAIResponse)

        // Then
        assertThat(intermittentResponse).isNotNull
        assertThat(intermittentResponse.id).isEqualTo("test-id")
        assertThat(intermittentResponse.model).isEqualTo("gpt-3.5-turbo")
        assertThat(intermittentResponse.choices).hasSize(1)
        assertThat(intermittentResponse.choices[0].message?.content).isEqualTo("Hello!")
        assertThat(intermittentResponse.usage?.totalTokens).isEqualTo(15)
    }

    @Test
    fun `should transform intermittent response to OpenAI response`() {
        // Given
        val intermittentResponse = IntermittentResponse(
            id = "test-id",
            model = "gpt-3.5-turbo",
            choices = listOf(
                com.fatihcure.kolo.core.IntermittentChoice(
                    index = 0,
                    message = com.fatihcure.kolo.core.IntermittentMessage(
                        role = com.fatihcure.kolo.core.MessageRole.ASSISTANT,
                        content = "Hello!",
                    ),
                    finishReason = "stop",
                ),
            ),
            usage = com.fatihcure.kolo.core.IntermittentUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15,
            ),
        )

        // When
        val openAIResponse = openAIProvider.transformResponse(intermittentResponse)

        // Then
        assertThat(openAIResponse).isNotNull
        assertThat(openAIResponse.id).isEqualTo("test-id")
        assertThat(openAIResponse.model).isEqualTo("gpt-3.5-turbo")
        assertThat(openAIResponse.choices).hasSize(1)
        assertThat(openAIResponse.choices[0].message?.content).isEqualTo("Hello!")
        assertThat(openAIResponse.usage?.totalTokens).isEqualTo(15)
    }

    @Test
    fun `should throw exception when normalizing streaming response`() {
        // Given
        val openAIResponse = OpenAIResponse(
            id = "test-id",
            model = "gpt-3.5-turbo",
            choices = emptyList(),
        )
        val stream = flowOf(openAIResponse)

        // When & Then
        assertThrows<UnsupportedOperationException> {
            runBlocking {
                openAIProvider.normalizeStreamingResponse(stream).first()
            }
        }
    }

    @Test
    fun `should transform streaming response with empty response`() = runBlocking {
        // Given
        val streamEvent = IntermittentStreamEvent.MessageStart("test-id", "gpt-3.5-turbo")
        val stream = flowOf(streamEvent)

        // When
        val result = openAIProvider.transformStreamingResponse(stream).first()

        // Then
        assertThat(result).isNotNull
        assertThat(result.id).isEmpty()
        assertThat(result.model).isEmpty()
        assertThat(result.choices).isEmpty()
        assertThat(result.usage).isNull()
    }

    @Test
    fun `should normalize error from OpenAI response`() {
        // Given
        val openAIResponse = OpenAIResponse(
            id = "error-id",
            model = "gpt-3.5-turbo",
            choices = emptyList(),
        )

        // When
        val intermittentError = openAIProvider.normalizeError(openAIResponse)

        // Then
        assertThat(intermittentError).isNotNull
        assertThat(intermittentError.type).isEqualTo("response_error")
        assertThat(intermittentError.message).isEqualTo("Error in response processing")
    }

    @Test
    fun `should transform error to OpenAI response`() {
        // Given
        val intermittentError = IntermittentError(
            type = "test_error",
            message = "Test error message",
        )

        // When
        val openAIResponse = openAIProvider.transformError(intermittentError)

        // Then
        assertThat(openAIResponse).isNotNull
        assertThat(openAIResponse.id).isEmpty()
        assertThat(openAIResponse.model).isEmpty()
        assertThat(openAIResponse.choices).isEmpty()
        assertThat(openAIResponse.usage).isNull()
    }

    @Test
    fun `should handle request with all optional parameters`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                OpenAIMessage(role = "system", content = "You are a helpful assistant"),
                OpenAIMessage(role = "user", content = "Hello!", name = "user1"),
            ),
            temperature = 0.8,
            maxTokens = 200,
            topP = 0.9,
            frequencyPenalty = 0.1,
            presencePenalty = 0.1,
            stop = listOf("STOP", "END"),
            stream = true,
        )

        // When
        val intermittentRequest = openAIProvider.normalizeRequest(openAIRequest)

        // Then
        assertThat(intermittentRequest).isNotNull
        assertThat(intermittentRequest.model).isEqualTo("gpt-4")
        assertThat(intermittentRequest.messages).hasSize(2)
        assertThat(intermittentRequest.temperature).isEqualTo(0.8)
        assertThat(intermittentRequest.maxTokens).isEqualTo(200)
        assertThat(intermittentRequest.topP).isEqualTo(0.9)
        assertThat(intermittentRequest.frequencyPenalty).isEqualTo(0.1)
        assertThat(intermittentRequest.presencePenalty).isEqualTo(0.1)
        assertThat(intermittentRequest.stop).containsExactly("STOP", "END")
        assertThat(intermittentRequest.stream).isTrue
    }

    @Test
    fun `should handle response with multiple choices`() {
        // Given
        val openAIResponse = OpenAIResponse(
            id = "multi-choice-id",
            model = "gpt-3.5-turbo",
            choices = listOf(
                OpenAIChoice(
                    index = 0,
                    message = OpenAIMessage(role = "assistant", content = "First choice"),
                    finishReason = "stop",
                ),
                OpenAIChoice(
                    index = 1,
                    message = OpenAIMessage(role = "assistant", content = "Second choice"),
                    finishReason = "length",
                ),
            ),
            usage = OpenAIUsage(
                promptTokens = 20,
                completionTokens = 10,
                totalTokens = 30,
            ),
        )

        // When
        val intermittentResponse = openAIProvider.normalizeResponse(openAIResponse)

        // Then
        assertThat(intermittentResponse).isNotNull
        assertThat(intermittentResponse.choices).hasSize(2)
        assertThat(intermittentResponse.choices[0].message?.content).isEqualTo("First choice")
        assertThat(intermittentResponse.choices[1].message?.content).isEqualTo("Second choice")
        assertThat(intermittentResponse.choices[0].finishReason).isEqualTo("stop")
        assertThat(intermittentResponse.choices[1].finishReason).isEqualTo("length")
    }
}
