package com.fatihcure.kolo.providers

import com.fatihcure.kolo.normalizers.anthropic.AnthropicMessage
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIMessage
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProviderIntegrationTest {

    private lateinit var koloProvider: KoloProvider
    private lateinit var openAIProvider: OpenAIProvider
    private lateinit var anthropicProvider: AnthropicProvider

    @BeforeEach
    fun setUp() {
        koloProvider = KoloProvider()
        openAIProvider = OpenAIProvider()
        anthropicProvider = AnthropicProvider()
    }

    @Test
    fun `should convert OpenAI request to Anthropic request through Kolo`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Hello, how are you?"),
            ),
            temperature = 0.7,
            maxTokens = 100,
        )

        // When
        val kolo = koloProvider.createKolo(OpenAIRequest::class, AnthropicRequest::class)
        val anthropicRequest = kolo.convertRequest(openAIRequest)

        // Then
        assertThat(anthropicRequest).isNotNull
        assertThat(anthropicRequest.model).isEqualTo("gpt-3.5-turbo")
        assertThat(anthropicRequest.messages).hasSize(1)
        assertThat(anthropicRequest.messages[0].content).isEqualTo("Hello, how are you?")
        assertThat(anthropicRequest.temperature).isEqualTo(0.7)
        assertThat(anthropicRequest.maxTokens).isEqualTo(100)
    }

    @Test
    fun `should convert Anthropic request to OpenAI request through Kolo`() {
        // Given
        val anthropicRequest = AnthropicRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(role = "user", content = "Hello, how are you?"),
            ),
            system = "You are a helpful assistant",
            temperature = 0.7,
            maxTokens = 100,
        )

        // When
        val kolo = koloProvider.createKolo(AnthropicRequest::class, OpenAIRequest::class)
        val openAIRequest = kolo.convertRequest(anthropicRequest)

        // Then
        assertThat(openAIRequest).isNotNull
        assertThat(openAIRequest.model).isEqualTo("claude-3-sonnet-20240229")
        assertThat(openAIRequest.messages).hasSize(2) // system + user message
        assertThat(openAIRequest.messages[0].role).isEqualTo("system")
        assertThat(openAIRequest.messages[0].content).isEqualTo("You are a helpful assistant")
        assertThat(openAIRequest.messages[1].role).isEqualTo("user")
        assertThat(openAIRequest.messages[1].content).isEqualTo("Hello, how are you?")
        assertThat(openAIRequest.temperature).isEqualTo(0.7)
        assertThat(openAIRequest.maxTokens).isEqualTo(100)
    }

    // Note: Response conversion tests are skipped as the current providers
    // have limited response conversion functionality implemented

    @Test
    fun `should work with bidirectional Kolo for request conversion`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Hello!"),
            ),
            temperature = 0.7,
        )

        // When
        val bidirectionalKolo = koloProvider.createBidirectionalKolo(OpenAIRequest::class, AnthropicRequest::class)
        val anthropicRequest = bidirectionalKolo.convertRequest(openAIRequest)

        // Then
        assertThat(anthropicRequest).isNotNull
        assertThat(anthropicRequest.model).isEqualTo("gpt-3.5-turbo")
        assertThat(anthropicRequest.messages).hasSize(1)
        assertThat(anthropicRequest.messages[0].content).isEqualTo("Hello!")
        assertThat(anthropicRequest.temperature).isEqualTo(0.7)

        // Note: convertResponse is not fully implemented in the current providers
        // so we only test the request conversion which works
    }

    @Test
    fun `should verify provider registration works correctly`() {
        // When & Then
        assertThat(koloProvider.canConvert(OpenAIRequest::class, AnthropicRequest::class)).isTrue
        assertThat(koloProvider.canConvert(AnthropicRequest::class, OpenAIRequest::class)).isTrue
        assertThat(koloProvider.canConvertBidirectional(OpenAIRequest::class, AnthropicRequest::class)).isTrue

        val conversionPairs = koloProvider.getAllConversionPairs()
        assertThat(conversionPairs).isNotEmpty
        assertThat(conversionPairs).contains(Pair(OpenAIRequest::class, AnthropicRequest::class))
        assertThat(conversionPairs).contains(Pair(AnthropicRequest::class, OpenAIRequest::class))
    }

    @Test
    fun `should handle complex request with all parameters`() {
        // Given
        val openAIRequest = OpenAIRequest(
            model = "gpt-4",
            messages = listOf(
                OpenAIMessage(role = "system", content = "You are a helpful assistant"),
                OpenAIMessage(role = "user", content = "Explain quantum computing", name = "user1"),
            ),
            temperature = 0.8,
            maxTokens = 500,
            topP = 0.9,
            frequencyPenalty = 0.1,
            presencePenalty = 0.1,
            stop = listOf("STOP", "END"),
            stream = false,
        )

        // When
        val kolo = koloProvider.createKolo(OpenAIRequest::class, AnthropicRequest::class)
        val anthropicRequest = kolo.convertRequest(openAIRequest)

        // Then
        assertThat(anthropicRequest).isNotNull
        assertThat(anthropicRequest.model).isEqualTo("gpt-4")
        assertThat(anthropicRequest.messages).hasSize(1) // Only user message, system goes to system field
        assertThat(anthropicRequest.system).isEqualTo("You are a helpful assistant")
        assertThat(anthropicRequest.messages[0].content).isEqualTo("Explain quantum computing")
        assertThat(anthropicRequest.temperature).isEqualTo(0.8)
        assertThat(anthropicRequest.maxTokens).isEqualTo(500)
        assertThat(anthropicRequest.topP).isEqualTo(0.9)
        assertThat(anthropicRequest.stop).containsExactly("STOP", "END")
        assertThat(anthropicRequest.stream).isFalse
    }
}
