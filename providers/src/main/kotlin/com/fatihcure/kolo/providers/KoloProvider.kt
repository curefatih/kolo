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
        val openAIProvider = OpenAIProvider(config = OpenAIProviderConfig.default())
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

        // Register normalizers for streaming event types
        val openAINormalizer = com.fatihcure.kolo.normalizers.openai.OpenAINormalizer()
        val openAIStreamingNormalizer = object : com.fatihcure.kolo.core.Normalizer<com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent> {
            override fun normalizeRequest(request: com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent): com.fatihcure.kolo.core.IntermittentRequest {
                throw UnsupportedOperationException("Streaming events don't have requests")
            }
            override fun normalizeResponse(response: com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent): com.fatihcure.kolo.core.IntermittentResponse {
                throw UnsupportedOperationException("Use normalizeStreamingResponse for streaming events")
            }
            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent>): kotlinx.coroutines.flow.Flow<com.fatihcure.kolo.core.IntermittentStreamEvent> {
                return openAINormalizer.normalizeStreamEvent(stream)
            }
            override fun normalizeError(error: com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent): com.fatihcure.kolo.core.IntermittentError {
                throw UnsupportedOperationException("Use normalizeStreamingResponse for streaming events")
            }
        }
        GlobalProviderAutoRegistration.registerNormalizer(
            com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent::class,
            openAIStreamingNormalizer,
        )

        val anthropicNormalizer = com.fatihcure.kolo.normalizers.anthropic.AnthropicNormalizer()
        val anthropicStreamingNormalizer = object : com.fatihcure.kolo.core.Normalizer<com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent> {
            override fun normalizeRequest(request: com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent): com.fatihcure.kolo.core.IntermittentRequest {
                throw UnsupportedOperationException("Streaming responses don't have requests")
            }
            override fun normalizeResponse(response: com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent): com.fatihcure.kolo.core.IntermittentResponse {
                throw UnsupportedOperationException("Use normalizeStreamingResponse for streaming responses")
            }
            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent>): kotlinx.coroutines.flow.Flow<com.fatihcure.kolo.core.IntermittentStreamEvent> {
                return anthropicNormalizer.normalizeStreamingResponse(stream)
            }
            override fun normalizeError(error: com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent): com.fatihcure.kolo.core.IntermittentError {
                throw UnsupportedOperationException("Use normalizeStreamingResponse for streaming responses")
            }
        }
        GlobalProviderAutoRegistration.registerNormalizer(
            com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent::class,
            anthropicStreamingNormalizer,
        )

        // Register streaming transformers for streaming event types
        val openAITransformer = com.fatihcure.kolo.transformers.openai.OpenAITransformer()
        GlobalProviderAutoRegistration.registerStreamingTransformer(
            com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent::class,
            openAITransformer as com.fatihcure.kolo.core.StreamingTransformer<com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent>,
        )

        val anthropicTransformer = com.fatihcure.kolo.transformers.anthropic.AnthropicTransformer()
        GlobalProviderAutoRegistration.registerStreamingTransformer(
            com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent::class,
            anthropicTransformer as com.fatihcure.kolo.core.StreamingTransformer<com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent>,
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
     * Creates a bidirectional Kolo instance using generic types with separate streaming response types
     */
    fun <SourceRequestType : Any, SourceResponseType : Any, SourceStreamingResponseType : Any, TargetRequestType : Any, TargetResponseType : Any, TargetStreamingResponseType : Any> createBidirectionalKolo(
        sourceRequestType: KClass<SourceRequestType>,
        sourceResponseType: KClass<SourceResponseType>,
        sourceStreamingResponseType: KClass<SourceStreamingResponseType>,
        targetRequestType: KClass<TargetRequestType>,
        targetResponseType: KClass<TargetResponseType>,
        targetStreamingResponseType: KClass<TargetStreamingResponseType>,
    ): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        return factory.createBidirectionalKolo(sourceRequestType, sourceResponseType, sourceStreamingResponseType, targetRequestType, targetResponseType, targetStreamingResponseType)
    }

    /**
     * Creates a Kolo instance using reified types
     */
    inline fun <reified SourceType : Any, reified TargetType : Any> createKolo(): Kolo<SourceType, TargetType> {
        return factory.createKolo<SourceType, TargetType>()
    }

    /**
     * Creates a bidirectional Kolo instance using reified types with separate streaming response types
     */
    inline fun <reified SourceRequestType : Any, reified SourceResponseType : Any, reified SourceStreamingResponseType : Any, reified TargetRequestType : Any, reified TargetResponseType : Any, reified TargetStreamingResponseType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        return factory.createBidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType>()
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
