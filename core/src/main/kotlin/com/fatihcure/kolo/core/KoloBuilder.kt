package com.fatihcure.kolo.core

/**
 * Builder class for creating Kolo instances
 */
class KoloBuilder<SourceType, TargetType> {
    private var sourceNormalizer: Normalizer<SourceType>? = null
    private var targetTransformer: Transformer<TargetType, TargetType, TargetType>? = null

    fun withSourceNormalizer(normalizer: Normalizer<SourceType>): KoloBuilder<SourceType, TargetType> {
        this.sourceNormalizer = normalizer
        return this
    }

    fun withTargetTransformer(transformer: Transformer<TargetType, TargetType, TargetType>): KoloBuilder<SourceType, TargetType> {
        this.targetTransformer = transformer
        return this
    }

    fun build(): Kolo<SourceType, TargetType> {
        require(sourceNormalizer != null) { "Source normalizer is required" }
        require(targetTransformer != null) { "Target transformer is required" }

        return Kolo(sourceNormalizer!!, targetTransformer!!)
    }
}

/**
 * Factory function for creating Kolo instances
 */
fun <SourceType, TargetType> kolo(
    sourceNormalizer: Normalizer<SourceType>,
    targetTransformer: Transformer<TargetType, TargetType, TargetType>,
): Kolo<SourceType, TargetType> {
    return Kolo(sourceNormalizer, targetTransformer)
}

/**
 * Factory function for creating KoloBuilder instances
 */
fun <SourceType, TargetType> koloBuilder(): KoloBuilder<SourceType, TargetType> {
    return KoloBuilder()
}
