package com.fatihcure.kolo.core

import kotlin.reflect.KClass

/**
 * Generic provider factory that can create providers dynamically based on type pairs
 */
class GenericProviderFactory(private val registry: ProviderRegistry) {

    constructor() : this(GlobalProviderRegistry.registry)

    /**
     * Create a Kolo instance for converting between different providers
     */
    fun <SourceRequestType : Any, SourceResponseType : Any, SourceStreamingResponseType : Any, SourceErrorType : Any, TargetRequestType : Any, TargetResponseType : Any, TargetStreamingResponseType : Any, TargetErrorType : Any> createKolo(
        sourceProviderType: KClass<*>,
        targetProviderType: KClass<*>,
    ): Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
        val sourceProvider = registry.getProvider<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType>(sourceProviderType)
            ?: throw IllegalArgumentException("No provider found for source type: ${sourceProviderType.simpleName}")

        val targetProvider = registry.getProvider<TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>(targetProviderType)
            ?: throw IllegalArgumentException("No provider found for target type: ${targetProviderType.simpleName}")

        return Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>(sourceProvider, targetProvider)
    }

    /**
     * Create a Kolo instance for converting between different providers using provider instances
     * This provides full compile-time safety without casting
     */
    fun <SourceRequestType : Any, SourceResponseType : Any, SourceStreamingResponseType : Any, SourceErrorType : Any, TargetRequestType : Any, TargetResponseType : Any, TargetStreamingResponseType : Any, TargetErrorType : Any> createKolo(
        sourceProviderType: KClass<*>,
        targetProviderType: KClass<*>,
        _sourceRequestType: KClass<SourceRequestType>,
        _sourceResponseType: KClass<SourceResponseType>,
        _sourceStreamingResponseType: KClass<SourceStreamingResponseType>,
        _sourceErrorType: KClass<SourceErrorType>,
        _targetRequestType: KClass<TargetRequestType>,
        _targetResponseType: KClass<TargetResponseType>,
        _targetStreamingResponseType: KClass<TargetStreamingResponseType>,
        _targetErrorType: KClass<TargetErrorType>,
    ): Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
        val sourceProvider = registry.getProvider<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType>(sourceProviderType)
            ?: throw IllegalArgumentException("No provider found for source type: ${sourceProviderType.simpleName}")

        val targetProvider = registry.getProvider<TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>(targetProviderType)
            ?: throw IllegalArgumentException("No provider found for target type: ${targetProviderType.simpleName}")

        return Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>(sourceProvider, targetProvider)
    }

    /**
     * Check if a conversion is possible from source to target
     */
    fun canConvert(
        sourceProviderType: KClass<*>,
        targetProviderType: KClass<*>,
    ): Boolean {
        return registry.hasProvider(sourceProviderType) && registry.hasProvider(targetProviderType)
    }

    /**
     * Get all possible target provider types for a given source provider type
     */
    fun getPossibleTargets(sourceProviderType: KClass<*>): Set<KClass<*>> {
        if (!registry.hasProvider(sourceProviderType)) {
            return emptySet()
        }

        return registry.getProviderTypes()
    }

    /**
     * Get all possible source provider types for a given target provider type
     */
    fun getPossibleSources(targetProviderType: KClass<*>): Set<KClass<*>> {
        if (!registry.hasProvider(targetProviderType)) {
            return emptySet()
        }

        return registry.getProviderTypes()
    }

    /**
     * Get all possible conversion pairs
     */
    fun getAllConversionPairs(): Set<Pair<KClass<*>, KClass<*>>> {
        val pairs = mutableSetOf<Pair<KClass<*>, KClass<*>>>()
        val providerTypes = registry.getProviderTypes()

        for (sourceType in providerTypes) {
            for (targetType in providerTypes) {
                if (sourceType != targetType) {
                    pairs.add(Pair(sourceType, targetType))
                }
            }
        }

        return pairs
    }
}

/**
 * Global provider factory instance
 */
object GlobalProviderFactory {
    val factory = GenericProviderFactory()

    fun <SourceRequestType : Any, SourceResponseType : Any, SourceStreamingResponseType : Any, SourceErrorType : Any, TargetRequestType : Any, TargetResponseType : Any, TargetStreamingResponseType : Any, TargetErrorType : Any> createKolo(
        sourceProviderType: KClass<*>,
        targetProviderType: KClass<*>,
    ): Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
        return factory.createKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>(sourceProviderType, targetProviderType)
    }

    fun canConvert(
        sourceProviderType: KClass<*>,
        targetProviderType: KClass<*>,
    ): Boolean {
        return factory.canConvert(sourceProviderType, targetProviderType)
    }

    fun getPossibleTargets(sourceProviderType: KClass<*>): Set<KClass<*>> {
        return factory.getPossibleTargets(sourceProviderType)
    }

    fun getPossibleSources(targetProviderType: KClass<*>): Set<KClass<*>> {
        return factory.getPossibleSources(targetProviderType)
    }

    fun getAllConversionPairs(): Set<Pair<KClass<*>, KClass<*>>> {
        return factory.getAllConversionPairs()
    }
}
