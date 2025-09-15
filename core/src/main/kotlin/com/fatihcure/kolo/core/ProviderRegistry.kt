package com.fatihcure.kolo.core

import kotlin.reflect.KClass

/**
 * Registry for managing streaming providers by provider class
 */
class ProviderRegistry {
    private val providers = mutableMapOf<KClass<*>, StreamingProvider<*, *, *, *>>()

    /**
     * Register a streaming provider by its provider class
     */
    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProvider(
        providerClass: KClass<*>,
        provider: StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        providers[providerClass] = provider
    }

    /**
     * Get a streaming provider for a specific provider class
     */
    @Suppress("UNCHECKED_CAST")
    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> getProvider(
        providerClass: KClass<*>,
    ): StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>? {
        return providers[providerClass] as? StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>
    }

    /**
     * Check if a provider exists for a specific provider class
     */
    fun hasProvider(providerClass: KClass<*>): Boolean {
        return providers.containsKey(providerClass)
    }

    /**
     * Get all registered provider types
     */
    fun getProviderTypes(): Set<KClass<*>> {
        return providers.keys.toSet()
    }

    /**
     * Get all registered providers
     */
    fun getAllProviders(): List<StreamingProvider<*, *, *, *>> {
        return providers.values.toList()
    }
}

/**
 * Global provider registry instance
 */
object GlobalProviderRegistry {
    val registry = ProviderRegistry()

    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProvider(
        providerClass: KClass<*>,
        provider: StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        registry.registerProvider(providerClass, provider)
    }

    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> getProvider(
        providerClass: KClass<*>,
    ): StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>? {
        return registry.getProvider(providerClass)
    }

    fun hasProvider(providerClass: KClass<*>): Boolean {
        return registry.hasProvider(providerClass)
    }

    fun getProviderTypes(): Set<KClass<*>> {
        return registry.getProviderTypes()
    }

    fun getAllProviders(): List<StreamingProvider<*, *, *, *>> {
        return registry.getAllProviders()
    }
}
