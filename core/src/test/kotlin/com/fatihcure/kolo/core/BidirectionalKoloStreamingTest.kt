package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BidirectionalKoloStreamingTest {

    @Test
    fun `convertStreamingResponse should work when streaming transformers are provided`() = runBlocking {
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

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                return stream.map { content ->
                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
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

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                return stream.map { content ->
                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
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

        val sourceStreamingTransformer = object : StreamingTransformer<String> {
            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<String> {
                return stream.map { event ->
                    when (event) {
                        is IntermittentStreamEvent.MessageDelta -> event.delta.content ?: ""
                        else -> ""
                    }
                }
            }
        }

        val targetStreamingTransformer = object : StreamingTransformer<String> {
            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<String> {
                return stream.map { event ->
                    when (event) {
                        is IntermittentStreamEvent.MessageDelta -> event.delta.content ?: ""
                        else -> ""
                    }
                }
            }
        }

        val bidirectionalKolo = BidirectionalKolo(
            sourceNormalizer = sourceNormalizer,
            targetNormalizer = targetNormalizer,
            sourceTransformer = sourceTransformer,
            targetTransformer = targetTransformer,
            sourceStreamingTransformer = sourceStreamingTransformer,
            targetStreamingTransformer = targetStreamingTransformer,
        )

        // Test streaming conversion
        val targetStream = flowOf("Hello", " World", "!")
        val resultStream = bidirectionalKolo.convertStreamingResponse(targetStream)
        val result = resultStream.toList()

        assertThat(result).containsExactly("Hello", " World", "!")
    }

    @Test
    fun `convertStreamingResponse should throw exception when source streaming transformer is missing`() = runBlocking {
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

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                return stream.map { content ->
                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
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

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                return stream.map { content ->
                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
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

        val targetStreamingTransformer = object : StreamingTransformer<String> {
            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<String> {
                return stream.map { event ->
                    when (event) {
                        is IntermittentStreamEvent.MessageDelta -> event.delta.content ?: ""
                        else -> ""
                    }
                }
            }
        }

        val bidirectionalKolo = BidirectionalKolo(
            sourceNormalizer = sourceNormalizer,
            targetNormalizer = targetNormalizer,
            sourceTransformer = sourceTransformer,
            targetTransformer = targetTransformer,
            sourceStreamingTransformer = null, // Missing source streaming transformer
            targetStreamingTransformer = targetStreamingTransformer,
        )

        // Test that streaming conversion throws exception
        val targetStream = flowOf("Hello", " World", "!")

        assertThatThrownBy {
            runBlocking {
                bidirectionalKolo.convertStreamingResponse(targetStream).toList()
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Source streaming transformer is required for streaming conversion")
    }

    @Test
    fun `convertStreamingResponse should throw exception when target streaming transformer is missing`() = runBlocking {
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

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                return stream.map { content ->
                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
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

            override fun normalizeStreamingResponse(stream: Flow<String>): Flow<IntermittentStreamEvent> {
                return stream.map { content ->
                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test_error",
                    message = error,
                )
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

        val sourceStreamingTransformer = object : StreamingTransformer<String> {
            override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<String> {
                return stream.map { event ->
                    when (event) {
                        is IntermittentStreamEvent.MessageDelta -> event.delta.content ?: ""
                        else -> ""
                    }
                }
            }
        }

        val bidirectionalKolo = BidirectionalKolo(
            sourceNormalizer = sourceNormalizer,
            targetNormalizer = targetNormalizer,
            sourceTransformer = sourceTransformer,
            targetTransformer = targetTransformer,
            sourceStreamingTransformer = sourceStreamingTransformer,
            targetStreamingTransformer = null, // Missing target streaming transformer
        )

        // Test that streaming conversion throws exception
        val targetStream = flowOf("Hello", " World", "!")

        assertThatThrownBy {
            runBlocking {
                bidirectionalKolo.convertStreamingResponse(targetStream).toList()
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Target streaming transformer is required for streaming conversion")
    }
}
