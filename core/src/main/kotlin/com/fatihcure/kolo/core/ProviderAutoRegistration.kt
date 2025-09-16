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
interface Provider<RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> {

    // Type information for compile-time safety
    val requestType: KClass<out RequestType>
    val responseType: KClass<out ResponseType>
    val streamingResponseType: KClass<out StreamEventType>
    val errorType: KClass<out ErrorType>

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
interface StreamingProvider<RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> :
    Provider<RequestType, ResponseType, StreamEventType, ErrorType> {

    /**
     * Process raw streaming data (e.g., from HTTP SSE response) and convert to Flow<IntermittentStreamEvent>
     * This method handles the actual streaming data processing with buffering and parsing
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of IntermittentStreamEvent objects
     */
    fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent>

    /**
     * Process intermittent stream event and convert to Flow<StreamEventType>
     * @param rawStream Flow of IntermittentStreamEvent data from the streaming response
     * @return Flow of StreamEventType objects
     */
    fun processStreamingDataToStreamEvent(stream: Flow<IntermittentStreamEvent>): Flow<StreamEventType>

    /**
     * Process raw streaming data and convert to Flow<StreamEventType>
     * This method handles the actual streaming data processing with buffering and parsing
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of StreamEventType objects
     */
    fun processRawStreamingDataToStreamEvent(rawStream: Flow<String>): Flow<StreamEventType>
}

/**
 * Provider auto-registration system that can automatically discover and register providers
 */
class ProviderAutoRegistration(private val registry: ProviderRegistry) {

    constructor() : this(GlobalProviderRegistry.registry)

    /**
     * Register a streaming provider by its provider class
     */
    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProvider(
        providerClass: KClass<*>,
        provider: StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        registry.registerProvider(providerClass, provider)
    }

    /**
     * Register multiple streaming providers at once
     * Note: This requires explicit type specification for each provider
     */
    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProviders(
        vararg providers: Pair<KClass<*>, StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>>,
    ) {
        providers.forEach { (type, provider) ->
            registry.registerProvider(type, provider)
        }
    }
}

/**
 * Global provider auto-registration instance
 */
object GlobalProviderAutoRegistration {
    private val autoRegistration = ProviderAutoRegistration()

    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProvider(
        providerClass: KClass<*>,
        provider: StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        autoRegistration.registerProvider(providerClass, provider)
    }

    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProviders(
        vararg providers: Pair<KClass<*>, StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>>,
    ) {
        autoRegistration.registerProviders(*providers)
    }
}
