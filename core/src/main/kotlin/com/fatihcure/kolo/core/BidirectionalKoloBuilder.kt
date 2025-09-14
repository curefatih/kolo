package com.fatihcure.kolo.core
/**
 * Builder class for creating BidirectionalKolo instances
 */
class BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
    private var sourceNormalizer: Normalizer<SourceRequestType>? = null
    private var sourceTransformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>? = null
    private var targetNormalizer: Normalizer<TargetResponseType>? = null
    private var targetTransformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>? = null
    private var sourceStreamingTransformer: StreamingTransformer<SourceResponseType>? = null
    private var targetStreamingTransformer: StreamingTransformer<TargetResponseType>? = null

    fun withSourceNormalizer(normalizer: Normalizer<SourceRequestType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        this.sourceNormalizer = normalizer
        return this
    }

    fun withSourceTransformer(transformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        this.sourceTransformer = transformer
        return this
    }

    fun withTargetNormalizer(normalizer: Normalizer<TargetResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        this.targetNormalizer = normalizer
        return this
    }

    fun withTargetTransformer(transformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        this.targetTransformer = transformer
        return this
    }

    fun withSourceStreamingTransformer(transformer: StreamingTransformer<SourceResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        this.sourceStreamingTransformer = transformer
        return this
    }

    fun withTargetStreamingTransformer(transformer: StreamingTransformer<TargetResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        this.targetStreamingTransformer = transformer
        return this
    }

    fun build(): BidirectionalKolo<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
        require(sourceNormalizer != null) { "Source normalizer is required" }
        require(sourceTransformer != null) { "Source transformer is required" }
        require(targetNormalizer != null) { "Target normalizer is required" }
        require(targetTransformer != null) { "Target transformer is required" }

        return BidirectionalKolo(
            sourceNormalizer!!,
            sourceTransformer!!,
            targetNormalizer!!,
            targetTransformer!!,
            sourceStreamingTransformer,
            targetStreamingTransformer,
        )
    }
}

/**
 * Factory function for creating BidirectionalKolo instances
 */
fun <SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> bidirectionalKolo(
    sourceNormalizer: Normalizer<SourceRequestType>,
    sourceTransformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>,
    targetNormalizer: Normalizer<TargetResponseType>,
    targetTransformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>,
    sourceStreamingTransformer: StreamingTransformer<SourceResponseType>? = null,
    targetStreamingTransformer: StreamingTransformer<TargetResponseType>? = null,
): BidirectionalKolo<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
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
 * Factory function for creating BidirectionalKoloBuilder instances
 */
fun <SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> bidirectionalKoloBuilder(): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> {
    return BidirectionalKoloBuilder()
}
