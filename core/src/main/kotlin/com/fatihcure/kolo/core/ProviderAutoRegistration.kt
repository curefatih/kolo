package com.fatihcure.kolo.core

// Provider interface will be defined in this file
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * Annotation to mark normalizers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class AutoRegisterNormalizer(val type: KClass<*>)

/**
 * Annotation to mark transformers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class AutoRegisterTransformer(val type: KClass<*>)

/**
 * Annotation to mark streaming transformers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class AutoRegisterStreamingTransformer(val type: KClass<*>)

/**
 * Annotation to mark providers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoRegisterProvider(val requestType: KClass<*>, val responseType: KClass<*>)

/**
 * Provider interface that combines normalizer and transformer functionality
 * for both request and response types
 */
interface Provider<RequestType, ResponseType, StreamEventType, ErrorType> {

    // Request normalization and transformation
    fun normalizeRequest(request: RequestType): IntermittentRequest
    fun transformRequest(request: IntermittentRequest): RequestType

    // Response normalization and transformation
    fun normalizeResponse(response: ResponseType): IntermittentResponse
    fun transformResponse(response: IntermittentResponse): ResponseType

    // Streaming support
    fun normalizeStreamingResponse(stream: Flow<StreamEventType>): Flow<IntermittentStreamEvent>
    fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<StreamEventType>

    // Error handling
    fun normalizeError(error: ErrorType): IntermittentError
    fun transformError(error: IntermittentError): ErrorType
}

/**
 * Enhanced provider interface that includes raw streaming data processing capabilities
 * This interface extends the base Provider interface with methods to handle raw streaming data
 * from HTTP responses (like Server-Sent Events) and convert them to the appropriate format
 */
interface StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType> : Provider<RequestType, ResponseType, StreamEventType, ErrorType> {
    
    /**
     * Process raw streaming data (e.g., from HTTP SSE response) and convert to Flow<IntermittentStreamEvent>
     * This method handles the actual streaming data processing with buffering and parsing
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of IntermittentStreamEvent objects
     */
    fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent>
    
    /**
     * Process raw streaming data and convert to Flow<StreamEventType>
     * This method handles the actual streaming data processing with buffering and parsing
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of StreamEventType objects
     */
    fun processStreamingDataToStreamEvent(rawStream: Flow<String>): Flow<StreamEventType>
    
    /**
     * Process raw HTTP response stream and convert to Flow<IntermittentStreamEvent>
     * This method handles the complete HTTP response processing including SSE parsing, filtering, and conversion
     * @param httpResponseStream Flow of raw HTTP response chunks
     * @return Flow of IntermittentStreamEvent objects
     */
    fun processHttpResponseStream(httpResponseStream: Flow<String>): Flow<IntermittentStreamEvent>
    
    /**
     * Process raw HTTP response stream and convert to Flow<StreamEventType>
     * This method handles the complete HTTP response processing including SSE parsing, filtering, and conversion
     * @param httpResponseStream Flow of raw HTTP response chunks
     * @return Flow of StreamEventType objects
     */
    fun processHttpResponseStreamToStreamEvent(httpResponseStream: Flow<String>): Flow<StreamEventType>
}

/**
 * Provider auto-registration system that can automatically discover and register providers
 */
class ProviderAutoRegistration(private val registry: ProviderRegistry) {

    constructor() : this(GlobalProviderRegistry.registry)

    /**
     * Register a normalizer with explicit type
     */
    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        registry.registerNormalizer(type, normalizer)
    }

    /**
     * Register a transformer with explicit type
     */
    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> registerTransformer(
        type: KClass<*>,
        transformer: Transformer<RequestType, ResponseType, ErrorType>,
    ) {
        registry.registerTransformer(type, transformer)
    }

    /**
     * Register multiple normalizers at once
     * Note: This requires explicit type specification for each normalizer
     */
    fun <T : Any> registerNormalizers(vararg normalizers: Pair<KClass<T>, Normalizer<T>>) {
        normalizers.forEach { (type, normalizer) ->
            registry.registerNormalizer(type, normalizer)
        }
    }

    /**
     * Register multiple transformers at once
     * Note: This requires explicit type specification for each transformer
     */
    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> registerTransformers(
        vararg transformers: Pair<KClass<*>, Transformer<RequestType, ResponseType, ErrorType>>,
    ) {
        transformers.forEach { (type, transformer) ->
            registry.registerTransformer(type, transformer)
        }
    }

    /**
     * Register a streaming transformer with explicit type
     */
    fun <StreamEventType : Any> registerStreamingTransformer(
        type: KClass<*>,
        transformer: StreamingTransformer<StreamEventType>,
    ) {
        registry.registerStreamingTransformer(type, transformer)
    }

    /**
     * Register multiple streaming transformers at once
     * Note: This requires explicit type specification for each streaming transformer
     */
    fun <StreamEventType : Any> registerStreamingTransformers(
        vararg transformers: Pair<KClass<*>, StreamingTransformer<StreamEventType>>,
    ) {
        transformers.forEach { (type, transformer) ->
            registry.registerStreamingTransformer(type, transformer)
        }
    }

    /**
     * Register a provider that implements both normalizer and transformer functionality
     */
    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProvider(
        requestType: KClass<RequestType>,
        responseType: KClass<ResponseType>,
        provider: Provider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        // Create adapter normalizers and transformers for the registry
        val requestNormalizer = object : Normalizer<RequestType> {
            override fun normalizeRequest(request: RequestType): IntermittentRequest = provider.normalizeRequest(request)
            override fun normalizeResponse(response: RequestType): IntermittentResponse = throw UnsupportedOperationException("Request type cannot be used as response")
            override fun normalizeStreamingResponse(stream: Flow<RequestType>): Flow<IntermittentStreamEvent> = throw UnsupportedOperationException("Request type cannot be used for streaming")
            override fun normalizeError(error: RequestType): IntermittentError = throw UnsupportedOperationException("Request type cannot be used for errors")
        }

        val requestTransformer = object : Transformer<RequestType, RequestType, RequestType> {
            override fun transformRequest(request: IntermittentRequest): RequestType = provider.transformRequest(request)
            override fun transformResponse(response: IntermittentResponse): RequestType = throw UnsupportedOperationException("Request type cannot be used as response")
            override fun transformError(error: IntermittentError): RequestType = throw UnsupportedOperationException("Request type cannot be used for errors")
        }

        val responseNormalizer = object : Normalizer<ResponseType> {
            override fun normalizeRequest(request: ResponseType): IntermittentRequest = throw UnsupportedOperationException("Response type cannot be used as request")
            override fun normalizeResponse(response: ResponseType): IntermittentResponse = provider.normalizeResponse(response)
            override fun normalizeStreamingResponse(stream: Flow<ResponseType>): Flow<IntermittentStreamEvent> = throw UnsupportedOperationException("Response type cannot be used for streaming - use StreamEventType")
            override fun normalizeError(error: ResponseType): IntermittentError = throw UnsupportedOperationException("Response type cannot be used for errors - use ErrorType")
        }

        val responseTransformer = object : Transformer<ResponseType, ResponseType, ResponseType> {
            override fun transformRequest(request: IntermittentRequest): ResponseType = throw UnsupportedOperationException("Response type cannot be used as request")
            override fun transformResponse(response: IntermittentResponse): ResponseType = provider.transformResponse(response)
            override fun transformError(error: IntermittentError): ResponseType = throw UnsupportedOperationException("Response type cannot be used for errors - use ErrorType")
        }

        registry.registerNormalizer(requestType, requestNormalizer as Normalizer<RequestType>)
        registry.registerTransformer(requestType, requestTransformer)
        registry.registerNormalizer(responseType, responseNormalizer as Normalizer<ResponseType>)
        registry.registerTransformer(responseType, responseTransformer)
    }

    /**
     * Register multiple providers at once
     */
    fun registerProviders(vararg providers: Triple<KClass<*>, KClass<*>, Provider<*, *, *, *>>) {
        providers.forEach { (requestType, responseType, provider) ->
            @Suppress("UNCHECKED_CAST")
            registerProvider(
                requestType as KClass<Any>,
                responseType as KClass<Any>,
                provider as Provider<Any, Any, Any, Any>,
            )
        }
    }

    /**
     * Auto-register all providers from a package
     * This would typically use reflection to scan for annotated classes
     */
    fun autoRegisterFromPackage(@Suppress("UNUSED_PARAMETER") packageName: String) {
        // This is a simplified version - in a real implementation,
        // you would use reflection to scan for annotated classes
        // and instantiate them automatically
        throw NotImplementedError("Auto-registration from package requires reflection implementation")
    }
}

/**
 * Global provider auto-registration instance
 */
object GlobalProviderAutoRegistration {
    private val autoRegistration = ProviderAutoRegistration()

    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        autoRegistration.registerNormalizer(type, normalizer)
    }

    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> registerTransformer(
        type: KClass<*>,
        transformer: Transformer<RequestType, ResponseType, ErrorType>,
    ) {
        autoRegistration.registerTransformer(type, transformer)
    }

    fun <T : Any> registerNormalizers(vararg normalizers: Pair<KClass<T>, Normalizer<T>>) {
        autoRegistration.registerNormalizers(*normalizers)
    }

    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> registerTransformers(
        vararg transformers: Pair<KClass<*>, Transformer<RequestType, ResponseType, ErrorType>>,
    ) {
        autoRegistration.registerTransformers(*transformers)
    }

    fun <StreamEventType : Any> registerStreamingTransformer(
        type: KClass<*>,
        transformer: StreamingTransformer<StreamEventType>,
    ) {
        autoRegistration.registerStreamingTransformer(type, transformer)
    }

    fun <StreamEventType : Any> registerStreamingTransformers(
        vararg transformers: Pair<KClass<*>, StreamingTransformer<StreamEventType>>,
    ) {
        autoRegistration.registerStreamingTransformers(*transformers)
    }

    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProvider(
        requestType: KClass<RequestType>,
        responseType: KClass<ResponseType>,
        provider: Provider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        autoRegistration.registerProvider(requestType, responseType, provider)
    }

    fun registerProviders(vararg providers: Triple<KClass<*>, KClass<*>, Provider<*, *, *, *>>) {
        autoRegistration.registerProviders(*providers)
    }
}
