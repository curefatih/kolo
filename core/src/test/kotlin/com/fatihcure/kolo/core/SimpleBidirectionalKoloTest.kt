package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleBidirectionalKoloTest {

    @Test
    fun `should create BidirectionalKolo instance`() {
        // Create mock normalizers and transformers
        val sourceNormalizer = object : Normalizer<String> {
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
                throw UnsupportedOperationException("Request type cannot be used as response")
            }

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                throw UnsupportedOperationException("Request type cannot be used for streaming")
            }

            override fun normalizeError(error: String): IntermittentError {
                throw UnsupportedOperationException("Request type cannot be used for errors")
            }
        }

        val sourceTransformer = object : Transformer<String, String, String> {
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

        val targetNormalizer = object : Normalizer<String> {
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
                throw UnsupportedOperationException()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError("test", error)
            }
        }

        val targetTransformer = object : Transformer<String, String, String> {
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

        // Create BidirectionalKolo instance
        val bidirectionalKolo = BidirectionalKolo(
            sourceNormalizer = sourceNormalizer,
            sourceTransformer = sourceTransformer,
            targetNormalizer = targetNormalizer,
            targetTransformer = targetTransformer,
        )

        // Test request conversion
        val sourceRequest = "Hello, world!"
        val targetRequest = bidirectionalKolo.convertRequest(sourceRequest)

        assertThat(targetRequest).isEqualTo("Hello, world!")

        // Test response conversion
        val targetResponse = "Hello back!"
        val sourceResponse = bidirectionalKolo.convertResponse(targetResponse)

        assertThat(sourceResponse).isEqualTo("Hello back!")
    }
}
