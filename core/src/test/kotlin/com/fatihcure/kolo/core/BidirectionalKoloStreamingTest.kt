package com.fatihcure.kolo.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

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

    @Test
    fun `convertStreamingResponse should process concurrently and not wait for all data`() = runBlocking {
        val processingDelays = mutableListOf<Long>()
        val processingOrder = mutableListOf<String>()

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
                    val startTime = System.currentTimeMillis()
                    processingOrder.add("normalize:$content")
                    // Simulate some processing delay
                    delay(50)
                    val endTime = System.currentTimeMillis()
                    processingDelays.add(endTime - startTime)

                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test-error",
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
                    val startTime = System.currentTimeMillis()
                    processingOrder.add("target-normalize:$content")
                    // Simulate some processing delay
                    delay(30)
                    val endTime = System.currentTimeMillis()
                    processingDelays.add(endTime - startTime)

                    IntermittentStreamEvent.MessageDelta(
                        delta = IntermittentDelta(
                            content = content,
                        ),
                    )
                }
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError(
                    type = "test-error",
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
                    val startTime = System.currentTimeMillis()
                    processingOrder.add("source-transform:$event")
                    // Simulate some processing delay
                    delay(40)
                    val endTime = System.currentTimeMillis()
                    processingDelays.add(endTime - startTime)

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

        // Create a stream with multiple items that will be processed
        val targetStream = flow {
            emit("Hello")
            delay(10) // Small delay between emissions
            emit(" World")
            delay(10)
            emit("!")
        }

        val totalTime = measureTimeMillis {
            val result = bidirectionalKolo.convertStreamingResponse(targetStream).toList()
            assertThat(result).containsExactly("Hello", " World", "!")
        }

        // Verify that processing happened concurrently by checking:
        // 1. Total time should be less than sum of all individual delays
        // 2. Processing order should show interleaved operations
        val totalSequentialTime = processingDelays.sum()

        // With concurrent processing, total time should be significantly less than sequential
        // Allow some tolerance for test execution overhead
        assertThat(totalTime).isLessThan((totalSequentialTime * 0.8).toLong())

        // Verify that we have processing from both normalization and transformation
        assertThat(processingOrder).contains("target-normalize:Hello")
        assertThat(processingOrder).contains("source-transform:")

        // The processing should be interleaved, not all normalization first, then all transformation
        val normalizeCount = processingOrder.count { it.startsWith("target-normalize:") }
        val transformCount = processingOrder.count { it.startsWith("source-transform:") }
        assertThat(normalizeCount).isGreaterThan(0)
        assertThat(transformCount).isGreaterThan(0)
    }
}
