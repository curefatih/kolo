package com.fatihcure.kolo.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoloTest {

    @Test
    fun `should create kolo instance with builder pattern`() {
        val normalizer = object : Normalizer<String> {
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

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
            }
        }

        val transformer = object : Transformer<String, String, String> {
            override fun transformRequest(request: IntermittentRequest): String {
                return request.messages.first().content
            }

            override fun transformResponse(response: IntermittentResponse): String {
                return response.choices.first().message?.content ?: ""
            }

            override fun transformError(error: IntermittentError): String {
                return error.message
            }
        }

        val kolo = koloBuilder<String, String>()
            .withSourceNormalizer(normalizer)
            .withTargetTransformer(transformer)
            .build()

        assertThat(kolo).isNotNull()
    }

    @Test
    fun `should create kolo instance with factory function`() {
        val normalizer = object : Normalizer<String> {
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

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
            }
        }

        val transformer = object : Transformer<String, String, String> {
            override fun transformRequest(request: IntermittentRequest): String {
                return request.messages.first().content
            }

            override fun transformResponse(response: IntermittentResponse): String {
                return response.choices.first().message?.content ?: ""
            }

            override fun transformError(error: IntermittentError): String {
                return error.message
            }
        }

        val kolo = kolo(normalizer, transformer)

        assertThat(kolo).isNotNull()
    }
}
