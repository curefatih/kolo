package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Bidirectional Kolo class that can convert in both directions
 */
class BidirectionalKolo<SourceType, TargetType>(
    private val sourceNormalizer: Normalizer<SourceType>,
    private val targetNormalizer: Normalizer<TargetType>,
    private val sourceTransformer: Transformer<SourceType, SourceType, SourceType>,
    private val targetTransformer: Transformer<TargetType, TargetType, TargetType>,
    private val sourceStreamingTransformer: StreamingTransformer<SourceType>? = null,
    private val targetStreamingTransformer: StreamingTransformer<TargetType>? = null,
) {
    companion object {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper().registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
    }

    /**
     * Converts a request from source format to target format
     */
    fun convertRequest(sourceRequest: SourceType): TargetType {
        val intermittentRequest = sourceNormalizer.normalizeRequest(sourceRequest)
        return targetTransformer.transformRequest(intermittentRequest)
    }

    /**
     * Converts a response from target format to source format
     */
    fun convertResponse(targetResponse: TargetType): SourceType {
        val intermittentResponse = targetNormalizer.normalizeResponse(targetResponse)
        return sourceTransformer.transformResponse(intermittentResponse)
    }

    /**
     * Converts a streaming response from target format to source format
     *
     * @param targetStream the streaming response from target format
     * @return the converted streaming response in source format
     * @throws UnsupportedOperationException if streaming transformers are not available
     */
    fun convertStreamingResponse(targetStream: Flow<TargetType>): Flow<SourceType> {
        requireNotNull(targetStreamingTransformer) {
            "Target streaming transformer is required for streaming conversion"
        }
        requireNotNull(sourceStreamingTransformer) {
            "Source streaming transformer is required for streaming conversion"
        }

        // First normalize the target stream to intermittent format
        val intermittentStream = targetNormalizer.normalizeStreamingResponse(targetStream)

        // Then transform the intermittent stream to source format
        return sourceStreamingTransformer.transformStreamingResponse(intermittentStream)
    }

    /**
     * Converts an error from target format to source format
     */
    fun convertError(targetError: TargetType): SourceType {
        val intermittentError = targetNormalizer.normalizeError(targetError)
        return sourceTransformer.transformError(intermittentError)
    }

    /**
     * Converts a request from JSON string to target format
     */
    inline fun <reified T> convertRequestFromJson(json: String): TargetType {
        val sourceRequest = objectMapper.readValue(json, T::class.java)
        return convertRequest(sourceRequest as SourceType)
    }

    /**
     * Converts a request from source format to JSON string
     */
    fun convertRequestToJson(sourceRequest: SourceType): String {
        return objectMapper.writeValueAsString(sourceRequest)
    }

    /**
     * Converts a response from target format to JSON string
     */
    fun convertResponseToJson(targetResponse: TargetType): String {
        return objectMapper.writeValueAsString(targetResponse)
    }

    /**
     * Converts a response from JSON string to source format
     */
    inline fun <reified T> convertResponseFromJson(json: String): SourceType {
        val targetResponse = objectMapper.readValue(json, T::class.java)
        return convertResponse(targetResponse as TargetType)
    }

    /**
     * Converts a streaming response from target format to JSON string
     */
    fun convertStreamingResponseToJson(targetStream: Flow<TargetType>): Flow<String> {
        return targetStream.map { response ->
            objectMapper.writeValueAsString(response)
        }
    }

    /**
     * Converts an error from target format to JSON string
     */
    fun convertErrorToJson(targetError: TargetType): String {
        return objectMapper.writeValueAsString(targetError)
    }

    /**
     * Converts an error from JSON string to source format
     */
    inline fun <reified T> convertErrorFromJson(json: String): SourceType {
        val targetError = objectMapper.readValue(json, T::class.java)
        return convertError(targetError as TargetType)
    }
}
