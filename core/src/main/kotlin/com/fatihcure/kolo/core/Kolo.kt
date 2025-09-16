package com.fatihcure.kolo.core

import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.flow.Flow

/**
 * Main Kolo class that orchestrates the conversion between different LLM providers
 * using the intermittent format as an intermediary
 */
class Kolo<
    SourceRequestType : Any,
    SourceResponseType : Any,
    SourceStreamingResponseType : Any,
    SourceErrorType : Any,
    TargetRequestType : Any,
    TargetResponseType : Any,
    TargetStreamingResponseType : Any,
    TargetErrorType : Any,
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

    /**
     * Process raw streaming data and convert to source streaming format
     */
    fun processRawStreamingToSourceStreaming(rawStream: Flow<String>): Flow<SourceStreamingResponseType> {
        return sourceProvider.processRawStreamingDataToStreamEvent(rawStream)
    }

    /**
     * Process raw streaming data and convert to target streaming format
     */
    fun processRawStreamingToTargetStreaming(rawStream: Flow<String>): Flow<TargetStreamingResponseType> {
        return targetProvider.processRawStreamingDataToStreamEvent(rawStream)
    }

    /**
     * Process raw streaming data through the full conversion pipeline
     * Source raw stream -> Intermittent -> Target streaming format
     */
    fun processRawStreamingThroughConversion(rawStream: Flow<String>): Flow<TargetStreamingResponseType> {
        val intermittentStream = sourceProvider.processStreamingData(rawStream)
        return targetProvider.processStreamingDataToStreamEvent(intermittentStream)
    }
}
