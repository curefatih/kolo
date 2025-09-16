package com.fatihcure.kolo.providers

import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test class for the provider-instance alternative approach
 * This demonstrates full compile-time safety without casting
 */
class ProviderInstanceTest {

    @Test
    fun `test createKolo with provider instances provides compile-time safety`() {
        // Create provider instances
        val openAIProvider = OpenAIProvider()
        val anthropicProvider = AnthropicProvider()

        // Create KoloProvider
        val koloProvider = KoloProvider()

        // Test the new provider-instance approach
        // This provides full compile-time safety without casting
        val kolo = koloProvider.createKolo(openAIProvider, anthropicProvider)

        // Verify the kolo instance is created successfully
        assertThat(kolo).isNotNull

        // Test type information access
        assertThat(openAIProvider.requestType).isEqualTo(OpenAIRequest::class)
        assertThat(openAIProvider.responseType).isEqualTo(OpenAIResponse::class)
        assertThat(anthropicProvider.requestType).isEqualTo(AnthropicRequest::class)
        assertThat(anthropicProvider.responseType).isEqualTo(AnthropicResponse::class)
    }

    @Test
    fun `test createKolo with reversed provider order`() {
        // Create provider instances
        val anthropicProvider = AnthropicProvider()
        val openAIProvider = OpenAIProvider()

        // Create KoloProvider
        val koloProvider = KoloProvider()

        // Test the new provider-instance approach with reversed order
        val kolo = koloProvider.createKolo(anthropicProvider, openAIProvider)

        // Verify the kolo instance is created successfully
        assertThat(kolo).isNotNull
    }

    @Test
    fun `test provider type information is accessible`() {
        // Create provider instances
        val openAIProvider = OpenAIProvider()
        val anthropicProvider = AnthropicProvider()

        // Test that type information is accessible from provider instances
        assertThat(openAIProvider.requestType).isNotNull
        assertThat(openAIProvider.responseType).isNotNull
        assertThat(openAIProvider.streamingResponseType).isNotNull
        assertThat(openAIProvider.errorType).isNotNull

        assertThat(anthropicProvider.requestType).isNotNull
        assertThat(anthropicProvider.responseType).isNotNull
        assertThat(anthropicProvider.streamingResponseType).isNotNull
        assertThat(anthropicProvider.errorType).isNotNull
    }
}
