package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.Kolo
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KoloProviderTest {

    private lateinit var koloProvider: KoloProvider

    @BeforeEach
    fun setUp() {
        koloProvider = KoloProvider()
    }

    @Test
    fun `should create Kolo instance for OpenAI to Anthropic conversion`() {
        // When
        val kolo = koloProvider.createKolo<OpenAIRequest, OpenAIResponse, com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent, com.fatihcure.kolo.normalizers.openai.OpenAIError, AnthropicRequest, AnthropicResponse, com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent, com.fatihcure.kolo.normalizers.anthropic.AnthropicError>(OpenAIRequest::class, AnthropicRequest::class)

        // Then
        assertThat(kolo).isNotNull()
        assertThat(kolo).isInstanceOf(Kolo::class.java)
    }

    @Test
    fun `should create Kolo instance for Anthropic to OpenAI conversion`() {
        // When
        val kolo = koloProvider.createKolo<AnthropicRequest, AnthropicResponse, com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent, com.fatihcure.kolo.normalizers.anthropic.AnthropicError, OpenAIRequest, OpenAIResponse, com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent, com.fatihcure.kolo.normalizers.openai.OpenAIError>(AnthropicRequest::class, OpenAIRequest::class)

        // Then
        assertThat(kolo).isNotNull()
        assertThat(kolo).isInstanceOf(Kolo::class.java)
    }

    @Test
    fun `should check if conversion is possible`() {
        // When & Then
        assertThat(koloProvider.canConvert(OpenAIRequest::class, AnthropicRequest::class)).isTrue()
        assertThat(koloProvider.canConvert(AnthropicRequest::class, OpenAIRequest::class)).isTrue()
    }

    @Test
    fun `should get possible target types for source type`() {
        // When
        val possibleTargets = koloProvider.getPossibleTargets(OpenAIRequest::class)

        // Then
        assertThat(possibleTargets).contains(AnthropicRequest::class)
    }

    @Test
    fun `should get possible source types for target type`() {
        // When
        val possibleSources = koloProvider.getPossibleSources(AnthropicRequest::class)

        // Then
        assertThat(possibleSources).contains(OpenAIRequest::class)
    }

    @Test
    fun `should get all conversion pairs`() {
        // When
        val conversionPairs = koloProvider.getAllConversionPairs()

        // Then
        assertThat(conversionPairs).isNotEmpty()
        assertThat(conversionPairs).contains(Pair(OpenAIRequest::class, AnthropicRequest::class))
        assertThat(conversionPairs).contains(Pair(AnthropicRequest::class, OpenAIRequest::class))
    }

    @Test
    fun `should have factory instance`() {
        // When & Then
        assertThat(koloProvider.factory).isNotNull()
    }
}
