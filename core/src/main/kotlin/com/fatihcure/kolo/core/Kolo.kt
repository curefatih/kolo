package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Main Kolo class that orchestrates the conversion between different LLM providers
 * using the intermittent format as an intermediary
 */
class Kolo<SourceType, TargetType>(
    private val sourceNormalizer: Normalizer<SourceType>,
    private val targetTransformer: Transformer<TargetType>,
) {
    val objectMapper = com.fasterxml.jackson.databind.ObjectMapper().registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())

    /**
     * Converts a request from source format to target format
     */
    fun convertRequest(sourceRequest: SourceType): TargetType {
        val intermittentRequest = sourceNormalizer.normalizeRequest(sourceRequest)
        return targetTransformer.transformRequest(intermittentRequest)
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
}

/**
 * Builder class for creating Kolo instances
 */
class KoloBuilder<SourceType, TargetType> {
    private var sourceNormalizer: Normalizer<SourceType>? = null
    private var targetTransformer: Transformer<TargetType>? = null

    fun withSourceNormalizer(normalizer: Normalizer<SourceType>): KoloBuilder<SourceType, TargetType> {
        this.sourceNormalizer = normalizer
        return this
    }

    fun withTargetTransformer(transformer: Transformer<TargetType>): KoloBuilder<SourceType, TargetType> {
        this.targetTransformer = transformer
        return this
    }

    fun build(): Kolo<SourceType, TargetType> {
        require(sourceNormalizer != null) { "Source normalizer is required" }
        require(targetTransformer != null) { "Target transformer is required" }

        return Kolo(sourceNormalizer!!, targetTransformer!!)
    }
}

/**
 * Factory function for creating Kolo instances
 */
fun <SourceType, TargetType> kolo(
    sourceNormalizer: Normalizer<SourceType>,
    targetTransformer: Transformer<TargetType>,
): Kolo<SourceType, TargetType> {
    return Kolo(sourceNormalizer, targetTransformer)
}

/**
 * Factory function for creating KoloBuilder instances
 */
fun <SourceType, TargetType> koloBuilder(): KoloBuilder<SourceType, TargetType> {
    return KoloBuilder()
}

/**
 * Bidirectional Kolo class that can convert in both directions
 */
class BidirectionalKolo<SourceType, TargetType>(
    private val sourceNormalizer: Normalizer<SourceType>,
    private val targetNormalizer: Normalizer<TargetType>,
    private val sourceTransformer: Transformer<SourceType>,
    private val targetTransformer: Transformer<TargetType>,
) {
    val objectMapper = com.fasterxml.jackson.databind.ObjectMapper().registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())

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
     */
    fun convertStreamingResponse(targetStream: Flow<TargetType>): Flow<SourceType> {
        val intermittentStream = targetNormalizer.normalizeStreamingResponse(targetStream)
        return sourceTransformer.transformStreamingResponse(intermittentStream)
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

/**
 * Builder class for creating BidirectionalKolo instances
 */
class BidirectionalKoloBuilder<SourceType, TargetType> {
    private var sourceNormalizer: Normalizer<SourceType>? = null
    private var targetNormalizer: Normalizer<TargetType>? = null
    private var sourceTransformer: Transformer<SourceType>? = null
    private var targetTransformer: Transformer<TargetType>? = null

    fun withSourceNormalizer(normalizer: Normalizer<SourceType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.sourceNormalizer = normalizer
        return this
    }

    fun withTargetNormalizer(normalizer: Normalizer<TargetType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.targetNormalizer = normalizer
        return this
    }

    fun withSourceTransformer(transformer: Transformer<SourceType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.sourceTransformer = transformer
        return this
    }

    fun withTargetTransformer(transformer: Transformer<TargetType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.targetTransformer = transformer
        return this
    }

    fun build(): BidirectionalKolo<SourceType, TargetType> {
        require(sourceNormalizer != null) { "Source normalizer is required" }
        require(targetNormalizer != null) { "Target normalizer is required" }
        require(sourceTransformer != null) { "Source transformer is required" }
        require(targetTransformer != null) { "Target transformer is required" }

        return BidirectionalKolo(
            sourceNormalizer!!,
            targetNormalizer!!,
            sourceTransformer!!,
            targetTransformer!!,
        )
    }
}

/**
 * Factory function for creating BidirectionalKolo instances
 */
fun <SourceType, TargetType> bidirectionalKolo(
    sourceNormalizer: Normalizer<SourceType>,
    targetNormalizer: Normalizer<TargetType>,
    sourceTransformer: Transformer<SourceType>,
    targetTransformer: Transformer<TargetType>,
): BidirectionalKolo<SourceType, TargetType> {
    return BidirectionalKolo(sourceNormalizer, targetNormalizer, sourceTransformer, targetTransformer)
}

/**
 * Factory function for creating BidirectionalKoloBuilder instances
 */
fun <SourceType, TargetType> bidirectionalKoloBuilder(): BidirectionalKoloBuilder<SourceType, TargetType> {
    return BidirectionalKoloBuilder()
}
