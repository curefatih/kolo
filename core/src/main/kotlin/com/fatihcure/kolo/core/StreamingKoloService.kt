package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * Service interface that provides easy access to streaming capabilities
 * This service allows users to process raw streaming data with minimal setup
 */
interface StreamingKoloService {
    
    /**
     * Process raw streaming data and convert to Flow<IntermittentStreamEvent>
     * This method handles the actual streaming data processing with buffering
     * @param rawStream Flow of raw string data from the streaming response
     * @param providerType The type of provider to use for processing
     * @return Flow of IntermittentStreamEvent objects
     */
    fun <StreamEventType : Any> processStreamingData(
        rawStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<IntermittentStreamEvent>
    
    /**
     * Process raw streaming data and convert to Flow<StreamEventType>
     * This method handles the actual streaming data processing with buffering
     * @param rawStream Flow of raw string data from the streaming response
     * @param providerType The type of provider to use for processing
     * @return Flow of StreamEventType objects
     */
    fun <StreamEventType : Any> processStreamingDataToStreamEvent(
        rawStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<StreamEventType>
    
    /**
     * Process raw HTTP response stream and convert to Flow<IntermittentStreamEvent>
     * This method handles the complete HTTP response processing including SSE parsing, filtering, and conversion
     * @param httpResponseStream Flow of raw HTTP response chunks
     * @param providerType The type of provider to use for processing
     * @return Flow of IntermittentStreamEvent objects
     */
    fun <StreamEventType : Any> processHttpResponseStream(
        httpResponseStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<IntermittentStreamEvent>
    
    /**
     * Process raw HTTP response stream and convert to Flow<StreamEventType>
     * This method handles the complete HTTP response processing including SSE parsing, filtering, and conversion
     * @param httpResponseStream Flow of raw HTTP response chunks
     * @param providerType The type of provider to use for processing
     * @return Flow of StreamEventType objects
     */
    fun <StreamEventType : Any> processHttpResponseStreamToStreamEvent(
        httpResponseStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<StreamEventType>
    
    /**
     * Check if a provider supports streaming data processing
     * @param providerType The type of provider to check
     * @return true if the provider supports streaming data processing
     */
    fun <StreamEventType : Any> supportsStreamingDataProcessing(providerType: KClass<StreamEventType>): Boolean
}

/**
 * Default implementation of StreamingKoloService
 */
class DefaultStreamingKoloService(
    private val providerRegistry: ProviderRegistry = GlobalProviderRegistry.registry
) : StreamingKoloService {
    
    override fun <StreamEventType : Any> processStreamingData(
        rawStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<IntermittentStreamEvent> {
        val provider = findStreamingProvider(providerType)
        return provider.processStreamingData(rawStream)
    }
    
    override fun <StreamEventType : Any> processStreamingDataToStreamEvent(
        rawStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<StreamEventType> {
        val provider = findStreamingProvider(providerType)
        return provider.processStreamingDataToStreamEvent(rawStream)
    }
    
    override fun <StreamEventType : Any> processHttpResponseStream(
        httpResponseStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<IntermittentStreamEvent> {
        val provider = findStreamingProvider(providerType)
        return provider.processHttpResponseStream(httpResponseStream)
    }
    
    override fun <StreamEventType : Any> processHttpResponseStreamToStreamEvent(
        httpResponseStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<StreamEventType> {
        val provider = findStreamingProvider(providerType)
        return provider.processHttpResponseStreamToStreamEvent(httpResponseStream)
    }
    
    override fun <StreamEventType : Any> supportsStreamingDataProcessing(providerType: KClass<StreamEventType>): Boolean {
        return try {
            findStreamingProvider(providerType)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <StreamEventType : Any> findStreamingProvider(providerType: KClass<StreamEventType>): StreamingProvider<*, *, StreamEventType, *> {
        // Find a provider that can handle this stream event type
        val providers = providerRegistry.getAllProviders()
        
        for (provider in providers) {
            if (provider is StreamingProvider<*, *, *, *>) {
                // Check if this provider can handle the stream event type
                // This is a simplified check - in a real implementation, you might need more sophisticated type checking
                try {
                    @Suppress("UNCHECKED_CAST")
                    return provider as StreamingProvider<*, *, StreamEventType, *>
                } catch (e: ClassCastException) {
                    // Continue to next provider
                }
            }
        }
        
        throw IllegalArgumentException("No streaming provider found for type: ${providerType.simpleName}")
    }
}

/**
 * Global instance of StreamingKoloService
 */
object GlobalStreamingKoloService {
    private val service = DefaultStreamingKoloService()
    
    fun <StreamEventType : Any> processStreamingData(
        rawStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<IntermittentStreamEvent> = service.processStreamingData(rawStream, providerType)
    
    fun <StreamEventType : Any> processStreamingDataToStreamEvent(
        rawStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<StreamEventType> = service.processStreamingDataToStreamEvent(rawStream, providerType)
    
    fun <StreamEventType : Any> processHttpResponseStream(
        httpResponseStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<IntermittentStreamEvent> = service.processHttpResponseStream(httpResponseStream, providerType)
    
    fun <StreamEventType : Any> processHttpResponseStreamToStreamEvent(
        httpResponseStream: Flow<String>,
        providerType: KClass<StreamEventType>
    ): Flow<StreamEventType> = service.processHttpResponseStreamToStreamEvent(httpResponseStream, providerType)
    
    fun <StreamEventType : Any> supportsStreamingDataProcessing(providerType: KClass<StreamEventType>): Boolean = 
        service.supportsStreamingDataProcessing(providerType)
}
