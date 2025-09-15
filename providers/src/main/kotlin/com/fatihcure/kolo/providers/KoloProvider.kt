package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.GlobalProviderAutoRegistration
import com.fatihcure.kolo.core.GlobalProviderFactory
import com.fatihcure.kolo.core.Kolo
import com.fatihcure.kolo.core.StreamingProvider
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent
import com.fatihcure.kolo.normalizers.anthropic.AnthropicError
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import kotlin.reflect.KClass

/**
 * Main Kolo provider class that demonstrates how to use the library
 * to convert between different LLM providers using the generic provider system
 */
class KoloProvider {

    val factory = GlobalProviderFactory.factory

    init {
        // Register all available providers
        registerProviders()
    }

    /**
     * Register all available providers with the global registry
     */
    private fun registerProviders() {
        val openAIProvider = OpenAIProvider(config = OpenAIProviderConfig.default())
        GlobalProviderAutoRegistration.registerProvider(
            OpenAIRequest::class,
            openAIProvider,
        )

        // Register Anthropic provider
        val anthropicProvider = AnthropicProvider()
        GlobalProviderAutoRegistration.registerProvider(
            AnthropicRequest::class,
            anthropicProvider,
        )
    }

    /**
     * Creates a Kolo instance for converting between different providers
     */
    fun <SourceRequestType : Any, SourceResponseType : Any, SourceStreamingResponseType : Any, SourceErrorType : Any, TargetRequestType : Any, TargetResponseType : Any, TargetStreamingResponseType : Any, TargetErrorType : Any> createKolo(
        sourceProviderType: KClass<*>,
        targetProviderType: KClass<*>,
    ): Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
        return factory.createKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>(sourceProviderType, targetProviderType)
    }


    /**
     * Check if a conversion is possible from source to target
     */
    fun canConvert(
        sourceProviderType: KClass<*>,
        targetProviderType: KClass<*>,
    ): Boolean {
        return factory.canConvert(sourceProviderType, targetProviderType)
    }

    /**
     * Get all possible target provider types for a given source provider type
     */
    fun getPossibleTargets(sourceProviderType: KClass<*>): Set<KClass<*>> {
        return factory.getPossibleTargets(sourceProviderType)
    }

    /**
     * Get all possible source provider types for a given target provider type
     */
    fun getPossibleSources(targetProviderType: KClass<*>): Set<KClass<*>> {
        return factory.getPossibleSources(targetProviderType)
    }

    /**
     * Get all possible conversion pairs
     */
    fun getAllConversionPairs(): Set<Pair<KClass<*>, KClass<*>>> {
        return factory.getAllConversionPairs()
    }
}
