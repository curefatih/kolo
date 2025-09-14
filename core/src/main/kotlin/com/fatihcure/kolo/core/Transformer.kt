package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow

/**
 * Interface for transforming intermittent format requests and responses
 * into provider-specific formats
 */
interface Transformer<RequestType, ResponseType, ErrorType> {
    /**
     * Transforms an intermittent request into a provider-specific request
     */
    fun transformRequest(request: IntermittentRequest): RequestType

    /**
     * Transforms an intermittent response into a provider-specific response
     */
    fun transformResponse(response: IntermittentResponse): ResponseType

    /**
     * Transforms an intermittent error into a provider-specific error
     */
    fun transformError(error: IntermittentError): ErrorType
}

/**
 * Interface for transforming intermittent streaming events
 * into provider-specific streaming event formats
 */
interface StreamingTransformer<StreamEventType> {
    /**
     * Transforms an intermittent streaming response into a provider-specific streaming response
     */
    fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<StreamEventType>
}

/**
 * Combined interface that includes both regular and streaming transformations
 */
interface CombinedTransformer<RequestType, ResponseType, ErrorType, StreamEventType> :
    Transformer<RequestType, ResponseType, ErrorType>, StreamingTransformer<StreamEventType>
