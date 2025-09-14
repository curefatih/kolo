package com.fatihcure.kolo.core

/**
 * Main Kolo class that orchestrates the conversion between different LLM providers
 * using the intermittent format as an intermediary
 */
class Kolo<SourceType, TargetType>(
    private val sourceNormalizer: Normalizer<SourceType>,
    private val targetTransformer: Transformer<TargetType, TargetType, TargetType>,
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
