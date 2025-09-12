package com.fatihcure.kolo.core

import kotlin.reflect.KClass

/**
 * Annotation to mark normalizers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoRegisterNormalizer(val type: KClass<*>)

/**
 * Annotation to mark transformers for auto-registration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoRegisterTransformer(val type: KClass<*>)

/**
 * Provider auto-registration system that can automatically discover and register providers
 */
class ProviderAutoRegistration(private val registry: ProviderRegistry) {
    
    constructor() : this(GlobalProviderRegistry.registry)
    
    /**
     * Register a normalizer with explicit type
     */
    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        registry.registerNormalizer(type, normalizer)
    }
    
    /**
     * Register a transformer with explicit type
     */
    fun <T : Any> registerTransformer(type: KClass<T>, transformer: Transformer<T>) {
        registry.registerTransformer(type, transformer)
    }
    
    /**
     * Register multiple normalizers at once
     * Note: This requires explicit type specification for each normalizer
     */
    fun <T : Any> registerNormalizers(vararg normalizers: Pair<KClass<T>, Normalizer<T>>) {
        normalizers.forEach { (type, normalizer) ->
            registry.registerNormalizer(type, normalizer)
        }
    }
    
    /**
     * Register multiple transformers at once
     * Note: This requires explicit type specification for each transformer
     */
    fun <T : Any> registerTransformers(vararg transformers: Pair<KClass<T>, Transformer<T>>) {
        transformers.forEach { (type, transformer) ->
            registry.registerTransformer(type, transformer)
        }
    }
    
    /**
     * Auto-register all providers from a package
     * This would typically use reflection to scan for annotated classes
     */
    fun autoRegisterFromPackage(packageName: String) {
        // This is a simplified version - in a real implementation,
        // you would use reflection to scan for annotated classes
        // and instantiate them automatically
        throw NotImplementedError("Auto-registration from package requires reflection implementation")
    }
}

/**
 * Global provider auto-registration instance
 */
object GlobalProviderAutoRegistration {
    private val autoRegistration = ProviderAutoRegistration()
    
    fun <T : Any> registerNormalizer(type: KClass<T>, normalizer: Normalizer<T>) {
        autoRegistration.registerNormalizer(type, normalizer)
    }
    
    fun <T : Any> registerTransformer(type: KClass<T>, transformer: Transformer<T>) {
        autoRegistration.registerTransformer(type, transformer)
    }
    
    fun <T : Any> registerNormalizers(vararg normalizers: Pair<KClass<T>, Normalizer<T>>) {
        autoRegistration.registerNormalizers(*normalizers)
    }
    
    fun <T : Any> registerTransformers(vararg transformers: Pair<KClass<T>, Transformer<T>>) {
        autoRegistration.registerTransformers(*transformers)
    }
}
