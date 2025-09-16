package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class KoloTest {

    @Test
    fun `should create kolo instance with builder pattern`() {
        val sourceProvider = createTestProvider()
        val targetProvider = createTestProvider()

        val kolo = koloBuilder<String, String, String, String, String, String, String, String>()
            .withSourceProvider(sourceProvider)
            .withTargetProvider(targetProvider)
            .build()

        assertThat(kolo).isNotNull()
    }

    @Test
    fun `should create kolo instance with factory function`() {
        val sourceProvider = createTestProvider()
        val targetProvider = createTestProvider()

        val kolo = kolo(sourceProvider, targetProvider)

        assertThat(kolo).isNotNull()
    }

    private fun createTestProvider(): StreamingProvider<String, String, String, String> {
        return object : StreamingProvider<String, String, String, String> {
            override val requestType: KClass<out String> = String::class
            override val responseType: KClass<out String> = String::class
            override val streamingResponseType: KClass<out String> = String::class
            override val errorType: KClass<out String> = String::class
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
    }
}
