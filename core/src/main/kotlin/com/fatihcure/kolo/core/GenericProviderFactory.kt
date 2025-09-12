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

        val targetTransformer = registry.getTransformer(targetType)
            ?: throw IllegalArgumentException("No transformer found for target type: ${targetType.simpleName}")

        return Kolo(sourceNormalizer, targetTransformer)
    }

    /**
     * Create a bidirectional Kolo instance for converting between source and target
     */
    fun <SourceType : Any, TargetType : Any> createBidirectionalKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): BidirectionalKolo<SourceType, TargetType> {
        val sourceNormalizer = registry.getNormalizer(sourceType)
            ?: throw IllegalArgumentException("No normalizer found for source type: ${sourceType.simpleName}")

        val targetNormalizer = registry.getNormalizer(targetType)
            ?: throw IllegalArgumentException("No normalizer found for target type: ${targetType.simpleName}")

        val sourceTransformer = registry.getTransformer(sourceType)
            ?: throw IllegalArgumentException("No transformer found for source type: ${sourceType.simpleName}")

        val targetTransformer = registry.getTransformer(targetType)
            ?: throw IllegalArgumentException("No transformer found for target type: ${targetType.simpleName}")

        return BidirectionalKolo(sourceNormalizer, targetNormalizer, sourceTransformer, targetTransformer)
    }

    /**
     * Create a unidirectional Kolo instance using reified types
     */
    inline fun <reified SourceType : Any, reified TargetType : Any> createKolo(): Kolo<SourceType, TargetType> {
        return createKolo(SourceType::class, TargetType::class)
    }

    /**
     * Create a bidirectional Kolo instance using reified types
     */
    inline fun <reified SourceType : Any, reified TargetType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceType, TargetType> {
        return createBidirectionalKolo(SourceType::class, TargetType::class)
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

    fun <SourceType : Any, TargetType : Any> createBidirectionalKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): BidirectionalKolo<SourceType, TargetType> {
        return factory.createBidirectionalKolo(sourceType, targetType)
    }

    inline fun <reified SourceType : Any, reified TargetType : Any> createKolo(): Kolo<SourceType, TargetType> {
        return factory.createKolo<SourceType, TargetType>()
    }

    inline fun <reified SourceType : Any, reified TargetType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceType, TargetType> {
        return factory.createBidirectionalKolo<SourceType, TargetType>()
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
