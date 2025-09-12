package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow

/**
 * Interface for transforming intermittent format requests and responses
 * into provider-specific formats
 */
interface Transformer<T> {
    /**
     * Transforms an intermittent request into a provider-specific request
     */
    fun transformRequest(request: IntermittentRequest): T

    /**
     * Transforms an intermittent response into a provider-specific response
     */
    fun transformResponse(response: IntermittentResponse): T

    /**
     * Transforms an intermittent streaming response into a provider-specific streaming response
     */
    fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<T>

    /**
     * Transforms an intermittent error into a provider-specific error
     */
    fun transformError(error: IntermittentError): T
}
