package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.GlobalProviderAutoRegistration
import com.fatihcure.kolo.core.GlobalProviderFactory
import com.fatihcure.kolo.core.Kolo
import com.fatihcure.kolo.core.StreamingProvider
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
            OpenAIProvider::class,
            openAIProvider,
        )

        // Register Anthropic provider
        val anthropicProvider = AnthropicProvider()
        GlobalProviderAutoRegistration.registerProvider(
            AnthropicProvider::class,
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
     * Creates a Kolo instance for converting between different providers using provider instances
     * This provides full compile-time safety without casting
     */
    inline fun <
        reified SP : StreamingProvider<SReq, SRes, SStream, SErr>,
        SReq : Any,
        SRes : Any,
        SStream : Any,
        SErr : Any,
        reified TP : StreamingProvider<TReq, TRes, TStream, TErr>,
        TReq : Any,
        TRes : Any,
        TStream : Any,
        TErr : Any,
        > createKolo(
        source: SP,
        target: TP,
    ): Kolo<SReq, SRes, SStream, SErr, TReq, TRes, TStream, TErr> {
        // Get runtime KClass from provider instances
        val sourceClass = source::class
        val targetClass = target::class

        // Use the existing factory method with KClass parameters
        return factory.createKolo<SReq, SRes, SStream, SErr, TReq, TRes, TStream, TErr>(
            sourceClass,
            targetClass,
        )
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
