package com.fatihcure.kolo.core

/**
 * Builder class for creating Kolo instances
 */
class KoloBuilder<
    SourceRequestType,
    SourceResponseType,
    SourceStreamingResponseType,
    SourceErrorType,
    TargetRequestType,
    TargetResponseType,
    TargetStreamingResponseType,
    TargetErrorType,
    > {
    private var sourceProvider: StreamingProvider<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType>? = null
    private var targetProvider: StreamingProvider<TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>? = null

    fun withSourceProvider(provider: StreamingProvider<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType>): KoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
        this.sourceProvider = provider
        return this
    }

    fun withTargetProvider(provider: StreamingProvider<TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>): KoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
        this.targetProvider = provider
        return this
    }

    fun build(): Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
        require(sourceProvider != null) { "Source provider is required" }
        require(targetProvider != null) { "Target provider is required" }

        return Kolo(sourceProvider!!, targetProvider!!)
    }
}

/**
 * Factory function for creating Kolo instances
 */
fun <
    SourceRequestType,
    SourceResponseType,
    SourceStreamingResponseType,
    SourceErrorType,
    TargetRequestType,
    TargetResponseType,
    TargetStreamingResponseType,
    TargetErrorType,
    > kolo(
    sourceProvider: StreamingProvider<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType>,
    targetProvider: StreamingProvider<TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType>,
): Kolo<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
    return Kolo(sourceProvider, targetProvider)
}

/**
 * Factory function for creating KoloBuilder instances
 */
fun <
    SourceRequestType,
    SourceResponseType,
    SourceStreamingResponseType,
    SourceErrorType,
    TargetRequestType,
    TargetResponseType,
    TargetStreamingResponseType,
    TargetErrorType,
    > koloBuilder(): KoloBuilder<SourceRequestType, SourceResponseType, SourceStreamingResponseType, SourceErrorType, TargetRequestType, TargetResponseType, TargetStreamingResponseType, TargetErrorType> {
    return KoloBuilder()
}
