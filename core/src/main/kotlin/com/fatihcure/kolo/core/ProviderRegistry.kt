package com.fatihcure.kolo.core

import kotlin.reflect.KClass

/**
 * Registry for managing streaming providers by type
 */
class ProviderRegistry {
    private val providers = mutableMapOf<KClass<*>, StreamingProvider<*, *, *, *>>()

    /**
     * Register a streaming provider for a specific type
     */
    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> registerProvider(
        type: KClass<*>,
        provider: StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        providers[type] = provider
    }

    /**
     * Get a streaming provider for a specific type
     */
    @Suppress("UNCHECKED_CAST")
    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> getProvider(
        type: KClass<*>,
    ): StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>? {
        return providers[type] as? StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>
    }

    /**
     * Check if a provider exists for a specific type
     */
    fun hasProvider(type: KClass<*>): Boolean {
        return providers.containsKey(type)
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
        type: KClass<*>,
        provider: StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>,
    ) {
        registry.registerProvider(type, provider)
    }

    fun <RequestType : Any, ResponseType : Any, StreamEventType : Any, ErrorType : Any> getProvider(
        type: KClass<*>,
    ): StreamingProvider<RequestType, ResponseType, StreamEventType, ErrorType>? {
        return registry.getProvider(type)
    }

    fun hasProvider(type: KClass<*>): Boolean {
        return registry.hasProvider(type)
    }

    fun getProviderTypes(): Set<KClass<*>> {
        return registry.getProviderTypes()
    }

    fun getAllProviders(): List<StreamingProvider<*, *, *, *>> {
        return registry.getAllProviders()
    }
}
