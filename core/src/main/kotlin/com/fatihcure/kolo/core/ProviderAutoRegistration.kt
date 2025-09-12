package com.fatihcure.kolo.core

// Provider interface will be defined in this file
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * Annotation to mark normalizers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoRegisterNormalizer(val type: KClass<*>)

/**
 * Annotation to mark transformers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoRegisterTransformer(val type: KClass<*>)

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
interface Provider<RequestType, ResponseType> {

    // Request normalization and transformation
    fun normalizeRequest(request: RequestType): IntermittentRequest
    fun transformRequest(request: IntermittentRequest): RequestType

    // Response normalization and transformation
    fun normalizeResponse(response: ResponseType): IntermittentResponse
    fun transformResponse(response: IntermittentResponse): ResponseType

    // Streaming support - simplified for now
    fun normalizeStreamingResponse(stream: Flow<ResponseType>): Flow<IntermittentStreamEvent>
    fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<ResponseType>

    // Error handling - simplified for now
    fun normalizeError(error: ResponseType): IntermittentError
    fun transformError(error: IntermittentError): ResponseType
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
    fun <T : Any> registerTransformer(type: KClass<T>, transformer: Transformer<T>) {
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
    fun <T : Any> registerTransformers(vararg transformers: Pair<KClass<T>, Transformer<T>>) {
        transformers.forEach { (type, transformer) ->
            registry.registerTransformer(type, transformer)
        }
    }

    /**
     * Register a provider that implements both normalizer and transformer functionality
     */
    fun <RequestType : Any, ResponseType : Any> registerProvider(
        requestType: KClass<RequestType>,
        responseType: KClass<ResponseType>,
        provider: Provider<RequestType, ResponseType>,
    ) {
        // Create adapter normalizers and transformers for the registry
        val requestNormalizer = object : Normalizer<RequestType> {
            override fun normalizeRequest(request: RequestType): IntermittentRequest = provider.normalizeRequest(request)
            override fun normalizeResponse(response: RequestType): IntermittentResponse = throw UnsupportedOperationException("Request type cannot be used as response")
            override fun normalizeStreamingResponse(stream: Flow<RequestType>): Flow<IntermittentStreamEvent> = throw UnsupportedOperationException("Request type cannot be used for streaming")
            override fun normalizeError(error: RequestType): IntermittentError = throw UnsupportedOperationException("Request type cannot be used for errors")
        }

        val requestTransformer = object : Transformer<RequestType> {
            override fun transformRequest(request: IntermittentRequest): RequestType = provider.transformRequest(request)
            override fun transformResponse(response: IntermittentResponse): RequestType = throw UnsupportedOperationException("Request type cannot be used as response")
            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<RequestType> = throw UnsupportedOperationException("Request type cannot be used for streaming")
            override fun transformError(error: IntermittentError): RequestType = throw UnsupportedOperationException("Request type cannot be used for errors")
        }

        val responseNormalizer = object : Normalizer<ResponseType> {
            override fun normalizeRequest(request: ResponseType): IntermittentRequest = throw UnsupportedOperationException("Response type cannot be used as request")
            override fun normalizeResponse(response: ResponseType): IntermittentResponse = provider.normalizeResponse(response)
            override fun normalizeStreamingResponse(stream: Flow<ResponseType>): Flow<IntermittentStreamEvent> = provider.normalizeStreamingResponse(stream)
            override fun normalizeError(error: ResponseType): IntermittentError = provider.normalizeError(error)
        }

        val responseTransformer = object : Transformer<ResponseType> {
            override fun transformRequest(request: IntermittentRequest): ResponseType = throw UnsupportedOperationException("Response type cannot be used as request")
            override fun transformResponse(response: IntermittentResponse): ResponseType = provider.transformResponse(response)
            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<ResponseType> = provider.transformStreamingResponse(stream)
            override fun transformError(error: IntermittentError): ResponseType = provider.transformError(error)
        }

        registry.registerNormalizer(requestType, requestNormalizer)
        registry.registerTransformer(requestType, requestTransformer)
        registry.registerNormalizer(responseType, responseNormalizer)
        registry.registerTransformer(responseType, responseTransformer)
    }

    /**
     * Register multiple providers at once
     */
    fun registerProviders(vararg providers: Triple<KClass<*>, KClass<*>, Provider<*, *>>) {
        providers.forEach { (requestType, responseType, provider) ->
            @Suppress("UNCHECKED_CAST")
            registerProvider(
                requestType as KClass<Any>,
                responseType as KClass<Any>,
                provider as Provider<Any, Any>,
            )
        }
    }

    /**
     * Auto-register all providers from a package
     * This would typically use reflection to scan for annotated classes
     */
    fun autoRegisterFromPackage(packageName: String) {
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

    fun <T : Any> registerTransformer(type: KClass<T>, transformer: Transformer<T>) {
        autoRegistration.registerTransformer(type, transformer)
    }

    fun <T : Any> registerNormalizers(vararg normalizers: Pair<KClass<T>, Normalizer<T>>) {
        autoRegistration.registerNormalizers(*normalizers)
    }

    fun <T : Any> registerTransformers(vararg transformers: Pair<KClass<T>, Transformer<T>>) {
        autoRegistration.registerTransformers(*transformers)
    }

    fun <RequestType : Any, ResponseType : Any> registerProvider(
        requestType: KClass<RequestType>,
        responseType: KClass<ResponseType>,
        provider: Provider<RequestType, ResponseType>,
    ) {
        autoRegistration.registerProvider(requestType, responseType, provider)
    }

    fun registerProviders(vararg providers: Triple<KClass<*>, KClass<*>, Provider<*, *>>) {
        autoRegistration.registerProviders(*providers)
    }
}
