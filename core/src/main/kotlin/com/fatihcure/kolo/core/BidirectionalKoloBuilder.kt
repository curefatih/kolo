package com.fatihcure.kolo.core
/**
 * Builder class for creating BidirectionalKolo instances
 */
class BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
    private var sourceNormalizer: Normalizer<SourceRequestType>? = null
    private var sourceTransformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>? = null
    private var sourceStreamingNormalizer: Normalizer<SourceStreamingResponseType>? = null
    private var sourceStreamingTransformer: StreamingTransformer<SourceStreamingResponseType>? = null
    private var targetNormalizer: Normalizer<TargetResponseType>? = null
    private var targetTransformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>? = null
    private var targetStreamingNormalizer: Normalizer<TargetStreamingResponseType>? = null
    private var targetStreamingTransformer: StreamingTransformer<TargetStreamingResponseType>? = null

    fun withSourceNormalizer(normalizer: Normalizer<SourceRequestType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.sourceNormalizer = normalizer
        return this
    }

    fun withSourceTransformer(transformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.sourceTransformer = transformer
        return this
    }

    fun withSourceStreamingNormalizer(normalizer: Normalizer<SourceStreamingResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.sourceStreamingNormalizer = normalizer
        return this
    }

    fun withSourceStreamingTransformer(transformer: StreamingTransformer<SourceStreamingResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.sourceStreamingTransformer = transformer
        return this
    }

    fun withTargetNormalizer(normalizer: Normalizer<TargetResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.targetNormalizer = normalizer
        return this
    }

    fun withTargetTransformer(transformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.targetTransformer = transformer
        return this
    }

    fun withTargetStreamingNormalizer(normalizer: Normalizer<TargetStreamingResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.targetStreamingNormalizer = normalizer
        return this
    }

    fun withTargetStreamingTransformer(transformer: StreamingTransformer<TargetStreamingResponseType>): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        this.targetStreamingTransformer = transformer
        return this
    }

    fun build(): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
        require(sourceNormalizer != null) { "Source normalizer is required" }
        require(sourceTransformer != null) { "Source transformer is required" }
        require(sourceStreamingNormalizer != null) { "Source streaming normalizer is required" }
        require(sourceStreamingTransformer != null) { "Source streaming transformer is required" }
        require(targetNormalizer != null) { "Target normalizer is required" }
        require(targetTransformer != null) { "Target transformer is required" }
        require(targetStreamingNormalizer != null) { "Target streaming normalizer is required" }
        require(targetStreamingTransformer != null) { "Target streaming transformer is required" }

        return BidirectionalKolo(
            sourceNormalizer!!,
            sourceTransformer!!,
            sourceStreamingNormalizer!!,
            sourceStreamingTransformer!!,
            targetNormalizer!!,
            targetTransformer!!,
            targetStreamingNormalizer!!,
            targetStreamingTransformer!!,
        )
    }
}

/**
 * Factory function for creating BidirectionalKolo instances
 */
fun <SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> bidirectionalKolo(
    sourceNormalizer: Normalizer<SourceRequestType>,
    sourceTransformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>,
    sourceStreamingNormalizer: Normalizer<SourceStreamingResponseType>,
    sourceStreamingTransformer: StreamingTransformer<SourceStreamingResponseType>,
    targetNormalizer: Normalizer<TargetResponseType>,
    targetTransformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>,
    targetStreamingNormalizer: Normalizer<TargetStreamingResponseType>,
    targetStreamingTransformer: StreamingTransformer<TargetStreamingResponseType>,
): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
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
 * Factory function for creating BidirectionalKoloBuilder instances
 */
fun <SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> bidirectionalKoloBuilder(): BidirectionalKoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, TargetRequestType, TargetResponseType, TargetStreamingResponseType> {
    return BidirectionalKoloBuilder()
}
