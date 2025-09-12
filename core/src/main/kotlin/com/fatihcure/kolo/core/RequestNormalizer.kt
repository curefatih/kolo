package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow

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
 * Interface for normalizing provider-specific streaming responses into the intermittent format
 */
interface StreamingNormalizer<T> {
    /**
     * Normalizes a provider-specific streaming response into the intermittent format
     */
    fun normalizeStreamingResponse(stream: Flow<T>): Flow<IntermittentStreamEvent>
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

/**
 * Interface for transforming intermittent format requests into provider-specific formats
 */
interface RequestTransformer<T> {
    /**
     * Transforms an intermittent request into a provider-specific request
     */
    fun transformRequest(request: IntermittentRequest): T
}

/**
 * Interface for transforming intermittent format responses into provider-specific formats
 */
interface ResponseTransformer<T> {
    /**
     * Transforms an intermittent response into a provider-specific response
     */
    fun transformResponse(response: IntermittentResponse): T
}

/**
 * Interface for transforming intermittent format errors into provider-specific error formats
 */
interface ErrorTransformer<T> {
    /**
     * Transforms an intermittent error into a provider-specific error
     */
    fun transformError(error: IntermittentError): T
}

/**
 * Interface for transforming intermittent format streaming events into provider-specific streaming events
 */
interface StreamingTransformer<T> {
    /**
     * Transforms an intermittent streaming response into a provider-specific streaming response
     */
    fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<T>
}
