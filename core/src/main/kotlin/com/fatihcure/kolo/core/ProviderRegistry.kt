package com.fatihcure.kolo.core

import kotlin.reflect.KClass

/**
 * Registry for managing normalizers and transformers by type
 */
class ProviderRegistry {
    private val normalizers = mutableMapOf<KClass<*>, Normalizer<*>>()
    private val transformers = mutableMapOf<KClass<*>, Transformer<*>>()

    /**
     * Register a normalizer for a specific type
     */
    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        normalizers[type] = normalizer
    }

    /**
     * Register a transformer for a specific type
     */
    fun <T : Any> registerTransformer(type: KClass<T>, transformer: Transformer<T>) {
        transformers[type] = transformer
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
    fun <T : Any> getTransformer(type: KClass<T>): Transformer<T>? {
        return transformers[type] as? Transformer<T>
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
}

/**
 * Global provider registry instance
 */
object GlobalProviderRegistry {
    val registry = ProviderRegistry()

    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        registry.registerNormalizer(type, normalizer)
    }

    fun <T : Any> registerTransformer(type: KClass<T>, transformer: Transformer<T>) {
        registry.registerTransformer(type, transformer)
    }

    fun <T : Any> getNormalizer(type: KClass<T>): Normalizer<T>? {
        return registry.getNormalizer(type)
    }

    fun <T : Any> getTransformer(type: KClass<T>): Transformer<T>? {
        return registry.getTransformer(type)
    }

    fun hasNormalizer(type: KClass<*>): Boolean {
        return registry.hasNormalizer(type)
    }

    fun hasTransformer(type: KClass<*>): Boolean {
        return registry.hasTransformer(type)
    }

    fun getNormalizerTypes(): Set<KClass<*>> {
        return registry.getNormalizerTypes()
    }

    fun getTransformerTypes(): Set<KClass<*>> {
        return registry.getTransformerTypes()
    }
}
