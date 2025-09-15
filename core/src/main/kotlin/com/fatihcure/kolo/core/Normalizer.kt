package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow

/**
 * Interface for normalizing provider-specific streaming responses into the intermittent format
 */
interface StreamingNormalizer<T> {
    /**
     * Normalizes a provider-specific streaming response into the intermittent format
     */
    fun normalizeStreamingResponse(stream: Flow<T>): Flow<IntermittentStreamEvent>
}

/**
 * Interface for normalizing provider-specific requests into the intermittent format
 */
interface RequestNormalizer<T> {
    /**
     * Normalizes a provider-specific request into the intermittent format
     */
    fun normalizeRequest(request: T): IntermittentRequest
}

/**
 * Interface for normalizing provider-specific responses into the intermittent format
 */
interface ResponseNormalizer<T> {
    /**
     * Normalizes a provider-specific response into the intermittent format
     */
    fun normalizeResponse(response: T): IntermittentResponse
}

/**
 * Interface for normalizing provider-specific errors into the intermittent format
 */
interface ErrorNormalizer<T> {
    /**
     * Normalizes a provider-specific error into the intermittent format
     */
    fun normalizeError(error: T): IntermittentError
}
