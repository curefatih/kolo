package com.fatihcure.kolo.core

import kotlin.reflect.KClass

/**
 * Registry for managing normalizers and transformers by type
 */
class ProviderRegistry {
    private val normalizers = mutableMapOf<KClass<*>, Normalizer<*>>()
    private val transformers = mutableMapOf<KClass<*>, Transformer<*, *, *>>()
    private val streamingTransformers = mutableMapOf<KClass<*>, StreamingTransformer<*>>()
    private val combinedTransformers = mutableMapOf<KClass<*>, CombinedTransformer<*, *, *, *>>()

    /**
     * Register a normalizer for a specific type
     */
    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        normalizers[type] = normalizer
    }

    /**
     * Register a transformer for a specific type
     */
    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> registerTransformer(
        type: KClass<*>,
        transformer: Transformer<RequestType, ResponseType, ErrorType>,
    ) {
        transformers[type] = transformer
    }

    /**
     * Register a streaming transformer for a specific type
     */
    fun <StreamEventType : Any> registerStreamingTransformer(
        type: KClass<*>,
        transformer: StreamingTransformer<StreamEventType>,
    ) {
        streamingTransformers[type] = transformer
    }

    /**
     * Register a combined transformer for a specific type
     */
    fun <RequestType : Any, ResponseType : Any, ErrorType : Any, StreamEventType : Any> registerCombinedTransformer(
        type: KClass<*>,
        transformer: CombinedTransformer<RequestType, ResponseType, ErrorType, StreamEventType>,
    ) {
        combinedTransformers[type] = transformer
        // Also register as separate transformers for backward compatibility
        transformers[type] = transformer
        streamingTransformers[type] = transformer
    }

    /**
     * Get a normalizer for a specific type
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getNormalizer(type: KClass<T>): Normalizer<T>? {
        return normalizers[type] as? Normalizer<T>
    }

    /**
     * Get a transformer for a specific type
     */
    @Suppress("UNCHECKED_CAST")
    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> getTransformer(
        type: KClass<*>,
    ): Transformer<RequestType, ResponseType, ErrorType>? {
        return transformers[type] as? Transformer<RequestType, ResponseType, ErrorType>
    }

    /**
     * Get a streaming transformer for a specific type
     */
    @Suppress("UNCHECKED_CAST")
    fun <StreamEventType : Any> getStreamingTransformer(
        type: KClass<*>,
    ): StreamingTransformer<StreamEventType>? {
        return streamingTransformers[type] as? StreamingTransformer<StreamEventType>
    }

    /**
     * Get a combined transformer for a specific type
     */
    @Suppress("UNCHECKED_CAST")
    fun <RequestType : Any, ResponseType : Any, ErrorType : Any, StreamEventType : Any> getCombinedTransformer(
        type: KClass<*>,
    ): CombinedTransformer<RequestType, ResponseType, ErrorType, StreamEventType>? {
        return combinedTransformers[type] as? CombinedTransformer<RequestType, ResponseType, ErrorType, StreamEventType>
    }

    /**
     * Check if a normalizer exists for a specific type
     */
    fun hasNormalizer(type: KClass<*>): Boolean {
        return normalizers.containsKey(type)
    }

    /**
     * Check if a transformer exists for a specific type
     */
    fun hasTransformer(type: KClass<*>): Boolean {
        return transformers.containsKey(type)
    }

    /**
     * Check if a streaming transformer exists for a specific type
     */
    fun hasStreamingTransformer(type: KClass<*>): Boolean {
        return streamingTransformers.containsKey(type)
    }

    /**
     * Check if a combined transformer exists for a specific type
     */
    fun hasCombinedTransformer(type: KClass<*>): Boolean {
        return combinedTransformers.containsKey(type)
    }

    /**
     * Get all registered normalizer types
     */
    fun getNormalizerTypes(): Set<KClass<*>> {
        return normalizers.keys.toSet()
    }

    /**
     * Get all registered transformer types
     */
    fun getTransformerTypes(): Set<KClass<*>> {
        return transformers.keys.toSet()
    }

    /**
     * Get all registered streaming transformer types
     */
    fun getStreamingTransformerTypes(): Set<KClass<*>> {
        return streamingTransformers.keys.toSet()
    }

    /**
     * Get all registered combined transformer types
     */
    fun getCombinedTransformerTypes(): Set<KClass<*>> {
        return combinedTransformers.keys.toSet()
    }
}

/**
 * Global provider registry instance
 */
object GlobalProviderRegistry {
    val registry = ProviderRegistry()

    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        registry.registerNormalizer(type, normalizer)
    }

    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> registerTransformer(
        type: KClass<*>,
        transformer: Transformer<RequestType, ResponseType, ErrorType>,
    ) {
        registry.registerTransformer(type, transformer)
    }

    fun <StreamEventType : Any> registerStreamingTransformer(
        type: KClass<*>,
        transformer: StreamingTransformer<StreamEventType>,
    ) {
        registry.registerStreamingTransformer(type, transformer)
    }

    fun <T : Any> getNormalizer(type: KClass<T>): Normalizer<T>? {
        return registry.getNormalizer(type)
    }

    fun <RequestType : Any, ResponseType : Any, ErrorType : Any> getTransformer(
        type: KClass<*>,
    ): Transformer<RequestType, ResponseType, ErrorType>? {
        return registry.getTransformer(type)
    }

    fun <StreamEventType : Any> getStreamingTransformer(
        type: KClass<*>,
    ): StreamingTransformer<StreamEventType>? {
        return registry.getStreamingTransformer(type)
    }

    fun hasNormalizer(type: KClass<*>): Boolean {
        return registry.hasNormalizer(type)
    }

    fun hasTransformer(type: KClass<*>): Boolean {
        return registry.hasTransformer(type)
    }

    fun hasStreamingTransformer(type: KClass<*>): Boolean {
        return registry.hasStreamingTransformer(type)
    }

    fun getNormalizerTypes(): Set<KClass<*>> {
        return registry.getNormalizerTypes()
    }

    fun getTransformerTypes(): Set<KClass<*>> {
        return registry.getTransformerTypes()
    }

    fun getStreamingTransformerTypes(): Set<KClass<*>> {
        return registry.getStreamingTransformerTypes()
    }
}
