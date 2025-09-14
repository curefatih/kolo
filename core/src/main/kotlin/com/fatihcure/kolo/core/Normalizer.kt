package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow

/**
 * TODO: create combined normalizer instead
 * Interface for normalizing provider-specific requests and responses
 * into the intermittent format
 */
interface Normalizer<T> {
    /**
     * Normalizes a provider-specific request into the intermittent format
     */
    fun normalizeRequest(request: T): IntermittentRequest

    /**
     * Normalizes a provider-specific response into the intermittent format
     */
    fun normalizeResponse(response: T): IntermittentResponse

    /**
     * Normalizes a provider-specific streaming response into the intermittent format
     */
    fun normalizeStreamingResponse(stream: Flow<T>): Flow<IntermittentStreamEvent>

    /**
     * Normalizes a provider-specific error into the intermittent format
     */
    fun normalizeError(error: T): IntermittentError
}
