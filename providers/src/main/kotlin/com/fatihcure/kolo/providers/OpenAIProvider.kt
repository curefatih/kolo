package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.AutoRegisterProvider
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.StreamingProvider
import com.fatihcure.kolo.normalizers.openai.OpenAIError
import com.fatihcure.kolo.normalizers.openai.OpenAINormalizer
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamingResponse
import com.fatihcure.kolo.normalizers.openai.createOpenAIStreamingHandler
import com.fatihcure.kolo.transformers.openai.OpenAITransformer
import kotlinx.coroutines.flow.Flow

/**
 * OpenAI provider implementation that combines normalizer and transformer
 */
@AutoRegisterProvider(OpenAIRequest::class, OpenAIResponse::class)
class OpenAIProvider(
    private val config: OpenAIProviderConfig = OpenAIProviderConfig.default(),
) : StreamingProvider<OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError> {

    private val normalizer = OpenAINormalizer()
    private val transformer = OpenAITransformer()
    private val streamingHandler = createOpenAIStreamingHandler(config.objectMapper, config.dataBufferFactory)

    override fun normalizeRequest(request: OpenAIRequest): IntermittentRequest {
        return normalizer.normalizeRequest(request)
    }

    override fun transformRequest(request: IntermittentRequest): OpenAIRequest {
        return transformer.transformRequest(request)
    }

    override fun normalizeResponse(response: OpenAIResponse): IntermittentResponse {
        return normalizer.normalizeResponse(response)
    }

    override fun transformResponse(response: IntermittentResponse): OpenAIResponse {
        return transformer.transformResponse(response)
    }

    override fun normalizeStreamingResponse(stream: Flow<OpenAIStreamEvent>): Flow<IntermittentStreamEvent> {
        return normalizer.normalizeStreamEvent(stream)
    }



    override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<OpenAIStreamEvent> {
        return transformer.transformStreamingResponse(stream)
    }

    /**
     * Process raw streaming data and convert to Flow<IntermittentStreamEvent>
     * This method handles the actual streaming data processing with buffering
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of IntermittentStreamEvent objects
     */
    override fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent> {
        val streamingResponses = streamingHandler.processStreamingData(rawStream)
        return normalizer.normalizeStreamingResponse(streamingResponses)
    }

    override fun processStreamingDataToStreamEvent(stream: Flow<IntermittentStreamEvent>): Flow<OpenAIStreamEvent> {
        TODO("Not yet implemented")
    }

    /**
     * Process raw streaming data and convert to Flow<OpenAIStreamEvent>
     * This method handles the actual streaming data processing with buffering
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of OpenAIStreamEvent objects
     */
    override fun processStreamingDataToStreamEvent(rawStream: Flow<String>): Flow<OpenAIStreamEvent> {
        val streamingResponses = streamingHandler.processStreamingData(rawStream)
        val intermittentStream = normalizer.normalizeStreamingResponse(streamingResponses)
        return transformer.transformStreamingResponse(intermittentStream)
    }

    /**
     * Process raw streaming data and convert to Flow<OpenAIStreamingResponse>
     * This method handles the actual streaming data processing with buffering
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of OpenAIStreamingResponse objects
     */
    fun processStreamingDataToStreamingResponse(rawStream: Flow<String>): Flow<OpenAIStreamingResponse> {
        return streamingHandler.processStreamingData(rawStream)
    }

    // Error handling
    override fun normalizeError(error: OpenAIError): IntermittentError {
        return normalizer.normalizeError(error)
    }

    override fun transformError(error: IntermittentError): OpenAIError {
        return transformer.transformError(error)
    }
}
