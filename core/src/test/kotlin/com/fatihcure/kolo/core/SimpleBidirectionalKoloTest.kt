package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleBidirectionalKoloTest {

    // Helper function to create BidirectionalKolo with same type for regular and streaming responses
    private fun <SourceRequestType, SourceResponseType, TargetRequestType, TargetResponseType> createBidirectionalKoloWithSameStreamingTypes(
        sourceNormalizer: Normalizer<SourceRequestType>,
        sourceTransformer: Transformer<SourceRequestType, SourceResponseType, SourceResponseType>,
        targetNormalizer: Normalizer<TargetResponseType>,
        targetTransformer: Transformer<TargetRequestType, TargetResponseType, TargetResponseType>,
    ): BidirectionalKolo<SourceRequestType, SourceResponseType, SourceResponseType, TargetRequestType, TargetResponseType, TargetResponseType> {
        return BidirectionalKolo(
            sourceNormalizer = sourceNormalizer,
            sourceTransformer = sourceTransformer,
            sourceStreamingNormalizer = sourceNormalizer as Normalizer<SourceResponseType>,
            sourceStreamingTransformer = object : StreamingTransformer<SourceResponseType> {
                override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<SourceResponseType> {
                    return stream.map { event ->
                        when (event) {
                            is IntermittentStreamEvent.MessageStart -> sourceTransformer.transformResponse(
                                IntermittentResponse(
                                    id = event.id,
                                    model = event.model,
                                    choices = listOf(IntermittentChoice(index = 0)),
                                ),
                            ) as SourceResponseType
                            is IntermittentStreamEvent.MessageDelta -> sourceTransformer.transformResponse(
                                IntermittentResponse(
                                    id = "",
                                    model = "",
                                    choices = listOf(IntermittentChoice(index = 0, delta = event.delta)),
                                ),
                            ) as SourceResponseType
                            is IntermittentStreamEvent.MessageEnd -> sourceTransformer.transformResponse(
                                IntermittentResponse(
                                    id = "",
                                    model = "",
                                    choices = listOf(IntermittentChoice(index = 0, finishReason = event.finishReason)),
                                    usage = event.usage,
                                ),
                            ) as SourceResponseType
                            is IntermittentStreamEvent.Error -> sourceTransformer.transformError(event.error) as SourceResponseType
                        }
                    }
                }
            },
            targetNormalizer = targetNormalizer,
            targetTransformer = targetTransformer,
            targetStreamingNormalizer = targetNormalizer as Normalizer<TargetResponseType>,
            targetStreamingTransformer = object : StreamingTransformer<TargetResponseType> {
                override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<TargetResponseType> {
                    return stream.map { event ->
                        when (event) {
                            is IntermittentStreamEvent.MessageStart -> targetTransformer.transformResponse(
                                IntermittentResponse(
                                    id = event.id,
                                    model = event.model,
                                    choices = listOf(IntermittentChoice(index = 0)),
                                ),
                            ) as TargetResponseType
                            is IntermittentStreamEvent.MessageDelta -> targetTransformer.transformResponse(
                                IntermittentResponse(
                                    id = "",
                                    model = "",
                                    choices = listOf(IntermittentChoice(index = 0, delta = event.delta)),
                                ),
                            ) as TargetResponseType
                            is IntermittentStreamEvent.MessageEnd -> targetTransformer.transformResponse(
                                IntermittentResponse(
                                    id = "",
                                    model = "",
                                    choices = listOf(IntermittentChoice(index = 0, finishReason = event.finishReason)),
                                    usage = event.usage,
                                ),
                            ) as TargetResponseType
                            is IntermittentStreamEvent.Error -> targetTransformer.transformError(event.error) as TargetResponseType
                        }
                    }
                }
            },
        )
    }

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
        val bidirectionalKolo = createBidirectionalKoloWithSameStreamingTypes(
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
