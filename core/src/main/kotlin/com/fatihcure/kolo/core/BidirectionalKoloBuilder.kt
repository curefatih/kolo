package com.fatihcure.kolo.core
/**
 * Builder class for creating BidirectionalKolo instances
 */
class BidirectionalKoloBuilder<SourceType, TargetType> {
    private var sourceNormalizer: Normalizer<SourceType>? = null
    private var targetNormalizer: Normalizer<TargetType>? = null
    private var sourceTransformer: Transformer<SourceType, SourceType, SourceType>? = null
    private var targetTransformer: Transformer<TargetType, TargetType, TargetType>? = null

    fun withSourceNormalizer(normalizer: Normalizer<SourceType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.sourceNormalizer = normalizer
        return this
    }

    fun withTargetNormalizer(normalizer: Normalizer<TargetType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.targetNormalizer = normalizer
        return this
    }

    fun withSourceTransformer(transformer: Transformer<SourceType, SourceType, SourceType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.sourceTransformer = transformer
        return this
    }

    fun withTargetTransformer(transformer: Transformer<TargetType, TargetType, TargetType>): BidirectionalKoloBuilder<SourceType, TargetType> {
        this.targetTransformer = transformer
        return this
    }

    fun build(): BidirectionalKolo<SourceType, TargetType> {
        require(sourceNormalizer != null) { "Source normalizer is required" }
        require(targetNormalizer != null) { "Target normalizer is required" }
        require(sourceTransformer != null) { "Source transformer is required" }
        require(targetTransformer != null) { "Target transformer is required" }

        return BidirectionalKolo(
            sourceNormalizer!!,
            targetNormalizer!!,
            sourceTransformer!!,
            targetTransformer!!,
        )
    }
}

/**
 * Factory function for creating BidirectionalKolo instances
 */
fun <SourceType, TargetType> bidirectionalKolo(
    sourceNormalizer: Normalizer<SourceType>,
    targetNormalizer: Normalizer<TargetType>,
    sourceTransformer: Transformer<SourceType, SourceType, SourceType>,
    targetTransformer: Transformer<TargetType, TargetType, TargetType>,
): BidirectionalKolo<SourceType, TargetType> {
    return BidirectionalKolo(sourceNormalizer, targetNormalizer, sourceTransformer, targetTransformer)
}

/**
 * Factory function for creating BidirectionalKoloBuilder instances
 */
fun <SourceType, TargetType> bidirectionalKoloBuilder(): BidirectionalKoloBuilder<SourceType, TargetType> {
    return BidirectionalKoloBuilder()
}
