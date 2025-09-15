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
    fun `should create Kolo instance for OpenAI to Anthropic conversion using provider classes`() {
        // When
        val kolo = koloProvider.createKolo<OpenAIRequest, OpenAIResponse, com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent, com.fatihcure.kolo.normalizers.openai.OpenAIError, AnthropicRequest, AnthropicResponse, com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent, com.fatihcure.kolo.normalizers.anthropic.AnthropicError>(OpenAIProvider::class, AnthropicProvider::class)

        // Then
        assertThat(kolo).isNotNull()
        assertThat(kolo).isInstanceOf(Kolo::class.java)
    }

    @Test
    fun `should create Kolo instance for Anthropic to OpenAI conversion using provider classes`() {
        // When
        val kolo = koloProvider.createKolo<AnthropicRequest, AnthropicResponse, com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent, com.fatihcure.kolo.normalizers.anthropic.AnthropicError, OpenAIRequest, OpenAIResponse, com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent, com.fatihcure.kolo.normalizers.openai.OpenAIError>(AnthropicProvider::class, OpenAIProvider::class)

        // Then
        assertThat(kolo).isNotNull()
        assertThat(kolo).isInstanceOf(Kolo::class.java)
    }

    @Test
    fun `should check if conversion is possible using provider classes`() {
        // When & Then
        assertThat(koloProvider.canConvert(OpenAIProvider::class, AnthropicProvider::class)).isTrue()
        assertThat(koloProvider.canConvert(AnthropicProvider::class, OpenAIProvider::class)).isTrue()
    }

    @Test
    fun `should get possible target types for source provider class`() {
        // When
        val possibleTargets = koloProvider.getPossibleTargets(OpenAIProvider::class)

        // Then
        assertThat(possibleTargets).contains(AnthropicProvider::class)
    }

    @Test
    fun `should get possible source types for target provider class`() {
        // When
        val possibleSources = koloProvider.getPossibleSources(AnthropicProvider::class)

        // Then
        assertThat(possibleSources).contains(OpenAIProvider::class)
    }

    @Test
    fun `should get all conversion pairs using provider classes`() {
        // When
        val conversionPairs = koloProvider.getAllConversionPairs()

        // Then
        assertThat(conversionPairs).isNotEmpty()
        assertThat(conversionPairs).contains(Pair(OpenAIProvider::class, AnthropicProvider::class))
        assertThat(conversionPairs).contains(Pair(AnthropicProvider::class, OpenAIProvider::class))
    }

    @Test
    fun `should have factory instance`() {
        // When & Then
        assertThat(koloProvider.factory).isNotNull()
    }
}
