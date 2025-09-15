package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.Provider
import com.fatihcure.kolo.normalizers.anthropic.AnthropicError
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent
import com.fatihcure.kolo.normalizers.openai.OpenAIError
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProviderTypealiasTest {

    @Test
    fun `should verify Provider typealias works correctly`() {
        // Given
        val openAIProvider = OpenAIProvider(config = OpenAIProviderConfig.default())
        val anthropicProvider = AnthropicProvider()

        // When & Then
        assertThat(openAIProvider).isInstanceOf(Provider::class.java)
        assertThat(anthropicProvider).isInstanceOf(Provider::class.java)

        // Verify the typealias points to the correct interface
        assertThat(Provider::class.java).isEqualTo(com.fatihcure.kolo.core.Provider::class.java)
    }

    @Test
    fun `should verify providers implement correct generic types`() {
        // Given
        val openAIProvider = OpenAIProvider(config = OpenAIProviderConfig.default())
        val anthropicProvider = AnthropicProvider()

        // When & Then
        // These should compile without issues, indicating correct generic type implementation
        val openAIProviderTyped: Provider<OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError> = openAIProvider
        val anthropicProviderTyped: Provider<AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError> = anthropicProvider

        assertThat(openAIProviderTyped).isNotNull()
        assertThat(anthropicProviderTyped).isNotNull()
    }
}
