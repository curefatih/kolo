package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.AutoRegisterProvider
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.StreamingProvider
import com.fatihcure.kolo.normalizers.anthropic.AnthropicError
import com.fatihcure.kolo.normalizers.anthropic.AnthropicNormalizer
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent
import com.fatihcure.kolo.transformers.anthropic.AnthropicTransformer
import kotlinx.coroutines.flow.Flow

/**
 * Anthropic provider implementation that combines normalizer and transformer
 */
@AutoRegisterProvider(AnthropicRequest::class, AnthropicResponse::class)
class AnthropicProvider : StreamingProvider<AnthropicRequest, AnthropicResponse, AnthropicStreamEvent, AnthropicError> {

    private val normalizer = AnthropicNormalizer()
    private val transformer = AnthropicTransformer()

    // Request normalization and transformation
    override fun normalizeRequest(request: AnthropicRequest): IntermittentRequest {
        return normalizer.normalizeRequest(request)
    }

    override fun transformRequest(request: IntermittentRequest): AnthropicRequest {
        return transformer.transformRequest(request)
    }

    // Response normalization and transformation
    override fun normalizeResponse(response: AnthropicResponse): IntermittentResponse {
        return normalizer.normalizeResponse(response)
    }

    override fun transformResponse(response: IntermittentResponse): AnthropicResponse {
        return transformer.transformResponse(response)
    }

    // Streaming support
    override fun normalizeStreamingResponse(stream: Flow<AnthropicStreamEvent>): Flow<IntermittentStreamEvent> {
        return normalizer.normalizeStreamingResponse(stream)
    }

    override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<AnthropicStreamEvent> {
        return transformer.transformStreamingResponse(stream)
    }

    // Error handling
    override fun normalizeError(error: AnthropicError): IntermittentError {
        return normalizer.normalizeError(error)
    }

    override fun transformError(error: IntermittentError): AnthropicError {
        return transformer.transformError(error)
    }

    // StreamingProvider specific methods
    override fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent> {
        // This would need to be implemented based on the specific streaming format
        // For now, we'll throw an error indicating it needs implementation
        throw NotImplementedError("processStreamingData needs to be implemented for Anthropic streaming format")
    }

    override fun processStreamingDataToStreamEvent(stream: Flow<IntermittentStreamEvent>): Flow<AnthropicStreamEvent> {
        return transformer.transformStreamingResponse(stream)
    }

    override fun processRawStreamingDataToStreamEvent(rawStream: Flow<String>): Flow<AnthropicStreamEvent> {
        // This would need to be implemented based on the specific streaming format
        // For now, we'll throw an error indicating it needs implementation
        throw NotImplementedError("processRawStreamingDataToStreamEvent needs to be implemented for Anthropic streaming format")
    }
}
