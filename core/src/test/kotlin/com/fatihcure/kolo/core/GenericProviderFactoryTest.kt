package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GenericProviderFactoryTest {

    private lateinit var registry: ProviderRegistry
    private lateinit var factory: GenericProviderFactory

    @BeforeEach
    fun setUp() {
        registry = ProviderRegistry()
        factory = GenericProviderFactory(registry)
    }

    @Test
    fun `should create kolo instance when providers are registered`() {
        // Register a streaming provider
        val provider = object : StreamingProvider<String, String, String, String> {
            override fun normalizeRequest(request: String): IntermittentRequest {
                return IntermittentRequest(
                    messages = listOf(
                        IntermittentMessage(
                            role = MessageRole.USER,
                            content = request,
                        ),
                    ),
                    model = "test-model",
                )
            }

            override fun transformRequest(request: IntermittentRequest): String {
                return request.messages.first().content
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
                                content = response,
                            ),
                        ),
                    ),
                )
            }

            override fun transformResponse(response: IntermittentResponse): String {
                return response.choices.first().message?.content ?: ""
            }

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                return flowOf()
            }

            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<String> {
                return flowOf()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
            }

            override fun transformError(error: IntermittentError): String {
                return error.message
            }

            override fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent> {
                return flowOf()
            }

            override fun processStreamingDataToStreamEvent(stream: Flow<IntermittentStreamEvent>): Flow<String> {
                return flowOf()
            }

            override fun processRawStreamingDataToStreamEvent(rawStream: Flow<String>): Flow<String> {
                return flowOf()
            }
        }

        registry.registerProvider(String::class, provider)

        // Create a Kolo instance
        val kolo = factory.createKolo<String, String, String, String, String, String, String, String>(String::class, String::class)

        assertThat(kolo).isNotNull()
    }

    @Test
    fun `should throw exception when source provider is not registered`() {
        // Register only target provider
        val targetProvider = createTestProvider()
        registry.registerProvider(Int::class, targetProvider)

        // Should throw exception when source provider is not registered
        try {
            factory.createKolo<String, String, String, String, Int, Int, Int, Int>(String::class, Int::class)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("No provider found for source type")
        }
    }

    @Test
    fun `should throw exception when target provider is not registered`() {
        // Register only source provider
        val sourceProvider = createTestProvider()
        registry.registerProvider(String::class, sourceProvider)

        // Should throw exception when target provider is not registered
        try {
            factory.createKolo<String, String, String, String, Int, Int, Int, Int>(String::class, Int::class)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("No provider found for target type")
        }
    }

    @Test
    fun `should check if conversion is possible`() {
        val provider1 = createTestProvider()
        val provider2 = createTestProvider()

        registry.registerProvider(String::class, provider1)
        registry.registerProvider(Int::class, provider2)

        assertThat(factory.canConvert(String::class, String::class)).isTrue()
        assertThat(factory.canConvert(String::class, Int::class)).isTrue()
        assertThat(factory.canConvert(String::class, Double::class)).isFalse()
    }

    @Test
    fun `should get possible targets for a source type`() {
        val provider1 = createTestProvider()
        val provider2 = createTestProvider()

        registry.registerProvider(String::class, provider1)
        registry.registerProvider(Int::class, provider2)

        val possibleTargets = factory.getPossibleTargets(String::class)
        assertThat(possibleTargets).containsExactlyInAnyOrder(String::class, Int::class)
    }

    private fun createTestProvider(): StreamingProvider<String, String, String, String> {
        return object : StreamingProvider<String, String, String, String> {
            override fun normalizeRequest(request: String): IntermittentRequest =
                IntermittentRequest(messages = emptyList(), model = "test")
            override fun transformRequest(request: IntermittentRequest): String = ""
            override fun normalizeResponse(response: String): IntermittentResponse =
                IntermittentResponse(id = "test", model = "test", choices = emptyList())
            override fun transformResponse(response: IntermittentResponse): String = ""
            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> = flowOf()
            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<String> = flowOf()
            override fun normalizeError(error: String): IntermittentError = IntermittentError("test", "test")
            override fun transformError(error: IntermittentError): String = ""
            override fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent> = flowOf()
            override fun processStreamingDataToStreamEvent(stream: Flow<IntermittentStreamEvent>): Flow<String> = flowOf()
            override fun processRawStreamingDataToStreamEvent(rawStream: Flow<String>): Flow<String> = flowOf()
        }
    }
}
