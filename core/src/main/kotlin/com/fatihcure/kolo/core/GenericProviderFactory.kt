package com.fatihcure.kolo.core

import kotlin.reflect.KClass

/**
 * Generic provider factory that can create providers dynamically based on type pairs
 */
class GenericProviderFactory(private val registry: ProviderRegistry) {

    constructor() : this(GlobalProviderRegistry.registry)

    /**
     * Create a unidirectional Kolo instance for converting from source to target
     */
    fun <SourceType : Any, TargetType : Any> createKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Kolo<SourceType, TargetType> {
        val sourceNormalizer = registry.getNormalizer(sourceType)
            ?: throw IllegalArgumentException("No normalizer found for source type: ${sourceType.simpleName}")

        val targetTransformer = registry.getTransformer<TargetType, TargetType, TargetType>(targetType)
            ?: throw IllegalArgumentException("No transformer found for target type: ${targetType.simpleName}")

        return Kolo(sourceNormalizer, targetTransformer)
    }

    /**
     * Create a bidirectional Kolo instance for converting between different request and response types with separate streaming types
     */
    fun <SourceRequestType : Any, SourceResponseType : Any, SourceStreamingResponseType : Any, TargetRequestType : Any, TargetResponseType : Any, TargetStreamingResponseType : Any> createBidirectionalKolo(
        sourceRequestType: KClass<SourceRequestType>,
        sourceResponseType: KClass<SourceResponseType>,
        sourceStreamingResponseType: KClass<SourceStreamingResponseType>,
        targetRequestType: KClass<TargetRequestType>,
        targetResponseType: KClass<TargetResponseType>,
        targetStreamingResponseType: KClass<TargetStreamingResponseType>,
    ): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        val sourceNormalizer = registry.getNormalizer(sourceRequestType)
            ?: throw IllegalArgumentException("No normalizer found for source request type: ${sourceRequestType.simpleName}")

        val sourceTransformer = registry.getTransformer<SourceRequestType, SourceResponseType, SourceResponseType>(sourceResponseType)
            ?: throw IllegalArgumentException("No transformer found for source response type: ${sourceResponseType.simpleName}")

        val sourceStreamingNormalizer = registry.getNormalizer(sourceStreamingResponseType)
            ?: throw IllegalArgumentException("No normalizer found for source streaming response type: ${sourceStreamingResponseType.simpleName}")

        val sourceStreamingTransformer = registry.getStreamingTransformer<SourceStreamingResponseType>(sourceStreamingResponseType)
            ?: throw IllegalArgumentException("No streaming transformer found for source streaming response type: ${sourceStreamingResponseType.simpleName}")

        val targetNormalizer = registry.getNormalizer(targetResponseType)
            ?: throw IllegalArgumentException("No normalizer found for target response type: ${targetResponseType.simpleName}")

        val targetTransformer = registry.getTransformer<TargetRequestType, TargetResponseType, TargetResponseType>(targetRequestType)
            ?: throw IllegalArgumentException("No transformer found for target request type: ${targetRequestType.simpleName}")

        val targetStreamingNormalizer = registry.getNormalizer(targetStreamingResponseType)
            ?: throw IllegalArgumentException("No normalizer found for target streaming response type: ${targetStreamingResponseType.simpleName}")

        val targetStreamingTransformer = registry.getStreamingTransformer<TargetStreamingResponseType>(targetStreamingResponseType)
            ?: throw IllegalArgumentException("No streaming transformer found for target streaming response type: ${targetStreamingResponseType.simpleName}")

        return BidirectionalKolo(
            sourceNormalizer,
            sourceTransformer,
            sourceStreamingNormalizer,
            sourceStreamingTransformer,
            targetNormalizer,
            targetTransformer,
            targetStreamingNormalizer,
            targetStreamingTransformer,
        )
    }

    /**
     * Create a unidirectional Kolo instance using reified types
     */
    inline fun <reified SourceType : Any, reified TargetType : Any> createKolo(): Kolo<SourceType, TargetType> {
        return createKolo(SourceType::class, TargetType::class)
    }

    /**
     * Create a bidirectional Kolo instance using reified types with separate streaming types
     */
    inline fun <reified SourceRequestType : Any, reified SourceResponseType : Any, reified SourceStreamingResponseType : Any, reified TargetRequestType : Any, reified TargetResponseType : Any, reified TargetStreamingResponseType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        return createBidirectionalKolo(SourceRequestType::class, SourceResponseType::class, SourceStreamingResponseType::class, TargetRequestType::class, TargetResponseType::class, TargetStreamingResponseType::class)
    }

    /**
     * Check if a conversion is possible from source to target
     */
    fun <SourceType : Any, TargetType : Any> canConvert(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Boolean {
        return registry.hasNormalizer(sourceType) && registry.hasTransformer(targetType)
    }

    /**
     * Check if a bidirectional conversion is possible
     */
    fun <SourceType : Any, TargetType : Any> canConvertBidirectional(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Boolean {
        return registry.hasNormalizer(sourceType) &&
            registry.hasNormalizer(targetType) &&
            registry.hasTransformer(sourceType) &&
            registry.hasTransformer(targetType)
    }

    /**
     * Get all possible target types for a given source type
     */
    fun <SourceType : Any> getPossibleTargets(sourceType: KClass<SourceType>): Set<KClass<*>> {
        if (!registry.hasNormalizer(sourceType)) {
            return emptySet()
        }

        return registry.getTransformerTypes()
    }

    /**
     * Get all possible source types for a given target type
     */
    fun <TargetType : Any> getPossibleSources(targetType: KClass<TargetType>): Set<KClass<*>> {
        if (!registry.hasTransformer(targetType)) {
            return emptySet()
        }

        return registry.getNormalizerTypes()
    }

    /**
     * Get all possible conversion pairs
     */
    fun getAllConversionPairs(): Set<Pair<KClass<*>, KClass<*>>> {
        val pairs = mutableSetOf<Pair<KClass<*>, KClass<*>>>()

        for (sourceType in registry.getNormalizerTypes()) {
            for (targetType in registry.getTransformerTypes()) {
                pairs.add(Pair(sourceType, targetType))
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

    fun <SourceType : Any, TargetType : Any> createKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Kolo<SourceType, TargetType> {
        return factory.createKolo(sourceType, targetType)
    }

    fun <SourceRequestType : Any, SourceResponseType : Any, SourceStreamingResponseType : Any, TargetRequestType : Any, TargetResponseType : Any, TargetStreamingResponseType : Any> createBidirectionalKolo(
        sourceRequestType: KClass<SourceRequestType>,
        sourceResponseType: KClass<SourceResponseType>,
        sourceStreamingResponseType: KClass<SourceStreamingResponseType>,
        targetRequestType: KClass<TargetRequestType>,
        targetResponseType: KClass<TargetResponseType>,
        targetStreamingResponseType: KClass<TargetStreamingResponseType>,
    ): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        return factory.createBidirectionalKolo(sourceRequestType, sourceResponseType, sourceStreamingResponseType, targetRequestType, targetResponseType, targetStreamingResponseType)
    }

    inline fun <reified SourceType : Any, reified TargetType : Any> createKolo(): Kolo<SourceType, TargetType> {
        return factory.createKolo<SourceType, TargetType>()
    }

    inline fun <reified SourceRequestType : Any, reified SourceResponseType : Any, reified SourceStreamingResponseType : Any, reified TargetRequestType : Any, reified TargetResponseType : Any, reified TargetStreamingResponseType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        return factory.createBidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType>()
    }

    fun <SourceType : Any, TargetType : Any> canConvert(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Boolean {
        return factory.canConvert(sourceType, targetType)
    }

    fun <SourceType : Any, TargetType : Any> canConvertBidirectional(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): Boolean {
        return factory.canConvertBidirectional(sourceType, targetType)
    }

    fun <SourceType : Any> getPossibleTargets(sourceType: KClass<SourceType>): Set<KClass<*>> {
        return factory.getPossibleTargets(sourceType)
    }

    fun <TargetType : Any> getPossibleSources(targetType: KClass<TargetType>): Set<KClass<*>> {
        return factory.getPossibleSources(targetType)
    }

    fun getAllConversionPairs(): Set<Pair<KClass<*>, KClass<*>>> {
        return factory.getAllConversionPairs()
    }
}
