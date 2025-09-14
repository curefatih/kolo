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
     * Create a bidirectional Kolo instance for converting between source and target
     * @deprecated This method is deprecated due to breaking changes in BidirectionalKolo signature.
     * Use createBidirectionalKoloV2 instead.
     */
    @Deprecated("Use createBidirectionalKoloV2 instead", ReplaceWith("createBidirectionalKoloV2(sourceType, targetType, sourceType, targetType)"))
    fun <SourceType : Any, TargetType : Any> createBidirectionalKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): BidirectionalKolo<SourceType, SourceType, TargetType, TargetType> {
        throw UnsupportedOperationException("This method is deprecated. Use createBidirectionalKoloV2 instead.")
    }

    /**
     * Create a bidirectional Kolo instance for converting between different request and response types
     */
    fun <SourceRequestType : Any, SourceResponseType : Any, TargetRequestType : Any, TargetResponseType : Any> createBidirectionalKoloV2(
        sourceRequestType: KClass<SourceRequestType>,
        sourceResponseType: KClass<SourceResponseType>,
        targetRequestType: KClass<TargetRequestType>,
        targetResponseType: KClass<TargetResponseType>,
    ): BidirectionalKolo<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        val sourceNormalizer = registry.getNormalizer(sourceRequestType)
            ?: throw IllegalArgumentException("No normalizer found for source request type: ${sourceRequestType.simpleName}")

        val sourceTransformer = registry.getTransformer<SourceRequestType, SourceResponseType, SourceResponseType>(sourceResponseType)
            ?: throw IllegalArgumentException("No transformer found for source response type: ${sourceResponseType.simpleName}")

        val targetNormalizer = registry.getNormalizer(targetResponseType)
            ?: throw IllegalArgumentException("No normalizer found for target response type: ${targetResponseType.simpleName}")

        val targetTransformer = registry.getTransformer<TargetRequestType, TargetResponseType, TargetResponseType>(targetRequestType)
            ?: throw IllegalArgumentException("No transformer found for target request type: ${targetRequestType.simpleName}")

        // Try to get streaming transformers if available
        val sourceStreamingTransformer = registry.getStreamingTransformer<SourceResponseType>(sourceResponseType)
        val targetStreamingTransformer = registry.getStreamingTransformer<TargetResponseType>(targetResponseType)

        return BidirectionalKolo(
            sourceNormalizer,
            sourceTransformer,
            targetNormalizer,
            targetTransformer,
            sourceStreamingTransformer,
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
     * Create a bidirectional Kolo instance using reified types
     * @deprecated This method is deprecated due to breaking changes in BidirectionalKolo signature.
     * Use createBidirectionalKoloV2 instead.
     */
    @Deprecated("Use createBidirectionalKoloV2 instead", ReplaceWith("createBidirectionalKoloV2<SourceType, SourceType, TargetType, TargetType>()"))
    inline fun <reified SourceType : Any, reified TargetType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceType, SourceType, TargetType, TargetType> {
        throw UnsupportedOperationException("This method is deprecated. Use createBidirectionalKoloV2 instead.")
    }

    /**
     * Create a bidirectional Kolo instance using reified types
     */
    inline fun <reified SourceRequestType : Any, reified SourceResponseType : Any, reified TargetRequestType : Any, reified TargetResponseType : Any> createBidirectionalKoloV2(): BidirectionalKolo<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        return createBidirectionalKoloV2(SourceRequestType::class, SourceResponseType::class, TargetRequestType::class, TargetResponseType::class)
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

    @Deprecated("Use createBidirectionalKoloV2 instead", ReplaceWith("createBidirectionalKoloV2(sourceType, targetType, sourceType, targetType)"))
    fun <SourceType : Any, TargetType : Any> createBidirectionalKolo(
        sourceType: KClass<SourceType>,
        targetType: KClass<TargetType>,
    ): BidirectionalKolo<SourceType, SourceType, TargetType, TargetType> {
        throw UnsupportedOperationException("This method is deprecated. Use createBidirectionalKoloV2 instead.")
    }

    fun <SourceRequestType : Any, SourceResponseType : Any, TargetRequestType : Any, TargetResponseType : Any> createBidirectionalKoloV2(
        sourceRequestType: KClass<SourceRequestType>,
        sourceResponseType: KClass<SourceResponseType>,
        targetRequestType: KClass<TargetRequestType>,
        targetResponseType: KClass<TargetResponseType>,
    ): BidirectionalKolo<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        return factory.createBidirectionalKoloV2(sourceRequestType, sourceResponseType, targetRequestType, targetResponseType)
    }

    inline fun <reified SourceType : Any, reified TargetType : Any> createKolo(): Kolo<SourceType, TargetType> {
        return factory.createKolo<SourceType, TargetType>()
    }

    @Deprecated("Use createBidirectionalKoloV2 instead", ReplaceWith("createBidirectionalKoloV2<SourceType, SourceType, TargetType, TargetType>()"))
    inline fun <reified SourceType : Any, reified TargetType : Any> createBidirectionalKolo(): BidirectionalKolo<SourceType, SourceType, TargetType, TargetType> {
        throw UnsupportedOperationException("This method is deprecated. Use createBidirectionalKoloV2 instead.")
    }

    inline fun <reified SourceRequestType : Any, reified SourceResponseType : Any, reified TargetRequestType : Any, reified TargetResponseType : Any> createBidirectionalKoloV2(): BidirectionalKolo<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        return factory.createBidirectionalKoloV2<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType>()
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
