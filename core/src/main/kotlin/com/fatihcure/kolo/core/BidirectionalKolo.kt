package com.fatihcure.kolo.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Bidirectional Kolo class that can convert between two different LLM providers
 *
 * This class handles the complete bidirectional conversion flow:
 * - Forward: SourceRequest → TargetRequest (via source normalizer + target transformer)
 * - Backward: TargetResponse → SourceResponse (via target normalizer + source transformer)
 *
 * @param SourceRequestType The source provider's request type (e.g., OpenAIRequest)
 * @param SourceResponseType The source provider's response type (e.g., OpenAIResponse)
 * @param SourceStreamingResponseType The source provider's streaming response type (e.g., OpenAIStreamingResponse)
 * @param TargetRequestType The target provider's request type (e.g., AnthropicRequest)
 * @param TargetResponseType The target provider's response type (e.g., AnthropicResponse)
 * @param TargetStreamingResponseType The target provider's streaming response type (e.g., AnthropicStreamEvent)
 */
class BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType>(
    private val sourceNormalizer: Normalizer<SourceRequestType>,
    private val sourceTransformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>,
    private val sourceStreamingNormalizer: Normalizer<SourceStreamingResponseType>,
    private val sourceStreamingTransformer: StreamingTransformer<SourceStreamingResponseType>,
    private val targetNormalizer: Normalizer<TargetResponseType>,
    private val targetTransformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>,
    private val targetStreamingNormalizer: Normalizer<TargetStreamingResponseType>,
    private val targetStreamingTransformer: StreamingTransformer<TargetStreamingResponseType>,
) {
    companion object {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper().registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
    }

    /**
     * Converts a request from source format to target format
     *
     * @param sourceRequest The request in source format
     * @return The request converted to target format
     */
    fun convertRequest(sourceRequest: SourceRequestType): TargetRequestType {
        val intermittentRequest = sourceNormalizer.normalizeRequest(sourceRequest)
        return targetTransformer.transformRequest(intermittentRequest)
    }

    /**
     * Converts a response from target format to source format
     *
     * @param targetResponse The response in target format
     * @return The response converted to source format
     */
    fun convertResponse(targetResponse: TargetResponseType): SourceResponseType {
        val intermittentResponse = targetNormalizer.normalizeResponse(targetResponse)
        return sourceTransformer.transformResponse(intermittentResponse)
    }

    /**
     * Converts a streaming response from target format to source format
     * Uses concurrent processing to avoid waiting for all streaming payloads
     *
     * @param targetStream the streaming response from target format
     * @return the converted streaming response in source format
     */
    fun convertStreamingResponse(targetStream: Flow<TargetStreamingResponseType>): Flow<SourceStreamingResponseType> {
        return targetStream
            .flowOn(Dispatchers.IO)
            .buffer()
            .let { stream ->
                val intermittentStream = targetStreamingNormalizer.normalizeStreamingResponse(stream)

                sourceStreamingTransformer.transformStreamingResponse(intermittentStream)
                    .flowOn(Dispatchers.Default)
            }
    }

    /**
     * Converts an error from target format to source format
     *
     * @param targetError The error in target format
     * @return The error converted to source format
     */
    fun convertError(targetError: TargetResponseType): SourceResponseType {
        val intermittentError = targetNormalizer.normalizeError(targetError)
        return sourceTransformer.transformError(intermittentError)
    }

    /**
     * Converts a request from JSON string to target format
     */
    inline fun <reified T> convertRequestFromJson(json: String): TargetRequestType {
        val sourceRequest = objectMapper.readValue(json, T::class.java)
        return convertRequest(sourceRequest as SourceRequestType)
    }

    /**
     * Converts a request from source format to JSON string
     */
    fun convertRequestToJson(sourceRequest: SourceRequestType): String {
        return objectMapper.writeValueAsString(sourceRequest)
    }

    /**
     * Converts a response from target format to JSON string
     */
    fun convertResponseToJson(targetResponse: TargetResponseType): String {
        return objectMapper.writeValueAsString(targetResponse)
    }

    /**
     * Converts a response from JSON string to source format
     */
    inline fun <reified T> convertResponseFromJson(json: String): SourceResponseType {
        val targetResponse = objectMapper.readValue(json, T::class.java)
        return convertResponse(targetResponse as TargetResponseType)
    }

    /**
     * Converts a streaming response from target format to JSON string
     */
    fun convertStreamingResponseToJson(targetStream: Flow<TargetStreamingResponseType>): Flow<String> {
        return targetStream.map { response ->
            objectMapper.writeValueAsString(response)
        }
    }

    /**
     * Converts an error from target format to JSON string
     */
    fun convertErrorToJson(targetError: TargetResponseType): String {
        return objectMapper.writeValueAsString(targetError)
    }

    /**
     * Converts an error from JSON string to source format
     */
    inline fun <reified T> convertErrorFromJson(json: String): SourceResponseType {
        val targetError = objectMapper.readValue(json, T::class.java)
        return convertError(targetError as TargetResponseType)
    }
}
