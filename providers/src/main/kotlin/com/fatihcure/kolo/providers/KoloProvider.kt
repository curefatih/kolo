package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.BidirectionalKolo
import com.fatihcure.kolo.core.GlobalProviderAutoRegistration
import com.fatihcure.kolo.core.GlobalProviderFactory
import com.fatihcure.kolo.core.Kolo
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
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
        // Register OpenAI provider
        val openAIProvider = OpenAIProvider()
        GlobalProviderAutoRegistration.registerProvider(
            OpenAIRequest::class,
            OpenAIResponse::class,
            openAIProvider,
        )

        // Register Anthropic provider
        val anthropicProvider = AnthropicProvider()
        GlobalProviderAutoRegistration.registerProvider(
            AnthropicRequest::class,
            AnthropicResponse::class,
            anthropicProvider,
        )
    }

    /**
     * Creates a Kolo instance for converting from source to target using generic types
     */
    fun <SourceType : Any, TargetType : Any> createKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Kolo<SourceType, TargetType> {
        return factory.createKolo(sourceType, targetType)
    }

    /**
     * Creates a bidirectional Kolo instance using generic types
     */
    fun <SourceType : Any, TargetType : Any> createBidirectionalKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): BidirectionalKolo<SourceType, TargetType> {
        return factory.createBidirectionalKolo(sourceType, targetType)
    }

    /**
     * Creates a Kolo instance using reified types
     */
    inline fun <reified SourceType : Any, reified TargetType : Any> createKolo(): Kolo<SourceType, TargetType> {
        return factory.createKolo<SourceType, TargetType>()
    }

    /**
     * Creates a bidirectional Kolo instance using reified types
     */
    inline fun <reified SourceType : Any, reified TargetType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceType, TargetType> {
        return factory.createBidirectionalKolo<SourceType, TargetType>()
    }

    /**
     * Check if a conversion is possible from source to target
     */
    fun <SourceType : Any, TargetType : Any> canConvert(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Boolean {
        return factory.canConvert(sourceType, targetType)
    }

    /**
     * Check if a bidirectional conversion is possible
     */
    fun <SourceType : Any, TargetType : Any> canConvertBidirectional(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Boolean {
        return factory.canConvertBidirectional(sourceType, targetType)
    }

    /**
     * Get all possible target types for a given source type
     */
    fun <SourceType : Any> getPossibleTargets(sourceType: KClass<SourceType>): Set<KClass<*>> {
        return factory.getPossibleTargets(sourceType)
    }

    /**
     * Get all possible source types for a given target type
     */
    fun <TargetType : Any> getPossibleSources(targetType: KClass<TargetType>): Set<KClass<*>> {
        return factory.getPossibleSources(targetType)
    }

    /**
     * Get all possible conversion pairs
     */
    fun getAllConversionPairs(): Set<Pair<KClass<*>, KClass<*>>> {
        return factory.getAllConversionPairs()
    }
}
