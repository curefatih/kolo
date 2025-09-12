package com.fatihcure.kolo.core

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach

class GenericProviderFactoryTest {
    
    private lateinit var registry: ProviderRegistry
    private lateinit var factory: GenericProviderFactory
    
    @BeforeEach
    fun setUp() {
        registry = ProviderRegistry()
        factory = GenericProviderFactory(registry)
    }
    
    @Test
    fun `should create kolo instance when normalizer and transformer are registered`() {
        // Register a normalizer and transformer
        val normalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest {
                return IntermittentRequest(
                    messages = listOf(
                        IntermittentMessage(
                            role = MessageRole.USER,
                            content = request
                        )
                    ),
                    model = "test-model"
                )
            }
            
            override fun normalizeResponse(response: String): IntermittentResponse {
                return IntermittentResponse(
                    id = "test-id",
                    model = "test-model",
                    choices = listOf(
                        IntermittentChoice(
                            index = 0,
                            message = IntermittentMessage(
                                role = MessageRole.ASSISTANT,
                                content = response
                            )
                        )
                    )
                )
            }
            
            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                return kotlinx.coroutines.flow.flowOf()
            }
            
            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error
                )
            }
        }
        
        val transformer = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String {
                return request.messages.first().content
            }
            
            override fun transformResponse(response: IntermittentResponse): String {
                return response.choices.first().message?.content ?: ""
            }
            
            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> {
                return kotlinx.coroutines.flow.flowOf()
            }
            
            override fun transformError(error: IntermittentError): String {
                return error.message
            }
        }
        
        registry.registerNormalizer(String::class, normalizer)
        registry.registerTransformer(String::class, transformer)
        
        // Create a Kolo instance
        val kolo = factory.createKolo(String::class, String::class)
        
        assertThat(kolo).isNotNull()
    }
    
    @Test
    fun `should throw exception when normalizer is not registered`() {
        val transformer = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String = ""
            override fun transformResponse(response: IntermittentResponse): String = ""
            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flowOf()
            override fun transformError(error: IntermittentError): String = ""
        }
        
        registry.registerTransformer(String::class, transformer)
        
        // Should throw exception when normalizer is not registered
        try {
            factory.createKolo(String::class, String::class)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("No normalizer found for source type")
        }
    }
    
    @Test
    fun `should throw exception when transformer is not registered`() {
        val normalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest = IntermittentRequest(messages = emptyList(), model = "test")
            override fun normalizeResponse(response: String): IntermittentResponse = IntermittentResponse(id = "test", model = "test", choices = emptyList())
            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> = kotlinx.coroutines.flow.flowOf()
            override fun normalizeError(error: String): IntermittentError = IntermittentError("test", "test")
        }
        
        registry.registerNormalizer(String::class, normalizer)
        
        // Should throw exception when transformer is not registered
        try {
            factory.createKolo(String::class, String::class)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("No transformer found for target type")
        }
    }
    
    @Test
    fun `should check if conversion is possible`() {
        val normalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest = IntermittentRequest(messages = emptyList(), model = "test")
            override fun normalizeResponse(response: String): IntermittentResponse = IntermittentResponse(id = "test", model = "test", choices = emptyList())
            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> = kotlinx.coroutines.flow.flowOf()
            override fun normalizeError(error: String): IntermittentError = IntermittentError("test", "test")
        }
        
        val transformer = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String = ""
            override fun transformResponse(response: IntermittentResponse): String = ""
            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flowOf()
            override fun transformError(error: IntermittentError): String = ""
        }
        
        registry.registerNormalizer(String::class, normalizer)
        registry.registerTransformer(String::class, transformer)
        
        assertThat(factory.canConvert(String::class, String::class)).isTrue()
        assertThat(factory.canConvert(String::class, Int::class)).isFalse()
    }
    
    @Test
    fun `should get possible targets for a source type`() {
        val normalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest = IntermittentRequest(messages = emptyList(), model = "test")
            override fun normalizeResponse(response: String): IntermittentResponse = IntermittentResponse(id = "test", model = "test", choices = emptyList())
            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> = kotlinx.coroutines.flow.flowOf()
            override fun normalizeError(error: String): IntermittentError = IntermittentError("test", "test")
        }
        
        val transformer1 = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String = ""
            override fun transformResponse(response: IntermittentResponse): String = ""
            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flowOf()
            override fun transformError(error: IntermittentError): String = ""
        }
        
        val transformer2 = object : Transformer<Int> {
            override fun transformRequest(request: IntermittentRequest): Int = 0
            override fun transformResponse(response: IntermittentResponse): Int = 0
            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flowOf()
            override fun transformError(error: IntermittentError): Int = 0
        }
        
        registry.registerNormalizer(String::class, normalizer)
        registry.registerTransformer(String::class, transformer1)
        registry.registerTransformer(Int::class, transformer2)
        
        val possibleTargets = factory.getPossibleTargets(String::class)
        assertThat(possibleTargets).containsExactlyInAnyOrder(String::class, Int::class)
    }
}
