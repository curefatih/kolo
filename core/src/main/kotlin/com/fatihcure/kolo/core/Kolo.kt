package com.fatihcure.kolo.core

import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.flow.Flow

/**
 * Main Kolo class that orchestrates the conversion between different LLM providers
 * using the intermittent format as an intermediary
 */
class Kolo<
    SourceRequestType,
    SourceResponseType,
    SourceStreamingResponseType,
    SourceErrorType,
    TargetRequestType,
    TargetResponseType,
    TargetStreamingResponseType,
    TargetErrorType,
    >(
    private val sourceProvider: StreamingProvider<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType>,
    private val targetProvider: StreamingProvider<TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>,
) {
    companion object {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
    }

    /**
     * Converts a request from source format to target format
     */
    fun convertSourceRequestToTarget(request: SourceRequestType): TargetRequestType {
        val intermittentRequest = sourceProvider.normalizeRequest(request)
        return targetProvider.transformRequest(intermittentRequest)
    }

    /**
     * Converts a request from source format JSON String to target format
     */
    inline fun <reified T : SourceRequestType> convertJsonStringSourceRequestToTarget(jsonString: String): TargetRequestType {
        val sourceRequest = objectMapper.readValue(jsonString, T::class.java)
        return convertSourceRequestToTarget(sourceRequest)
    }

    /**
     * Converts a response from target format to source format
     */
    fun convertTargetResponseToSource(target: TargetResponseType): SourceResponseType {
        val intermittentResponse = targetProvider.normalizeResponse(target)
        return sourceProvider.transformResponse(intermittentResponse)
    }

    /**
     * Converts a response from target format JSON String to source format
     */
    inline fun <reified T : TargetResponseType> convertJsonStringResponseToTarget(jsonString: String): SourceResponseType {
        val sourceResponse = objectMapper.readValue(jsonString, T::class.java)
        return convertTargetResponseToSource(sourceResponse)
    }

    fun processSourceStreamingToTargetStreaming(rawStream: Flow<String>): Flow<TargetStreamingResponseType> {
        return sourceProvider.processStreamingData(rawStream)
            .let { targetProvider.processStreamingDataToStreamEvent(it) }
    }
}
