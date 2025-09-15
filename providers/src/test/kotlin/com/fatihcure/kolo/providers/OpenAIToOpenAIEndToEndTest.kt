package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.Kolo
import com.fatihcure.kolo.normalizers.openai.OpenAIError
import com.fatihcure.kolo.normalizers.openai.OpenAIMessage
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

/**
 * End-to-end test for OpenAI to OpenAI conversion using test data
 * This test mocks WebClient responses with random sequential chunks from test_data.json
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenAIToOpenAIEndToEndTest {

    private lateinit var sourceProvider: OpenAIProvider
    private lateinit var targetProvider: OpenAIProvider
    private lateinit var kolo: Kolo<OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError, OpenAIRequest, OpenAIResponse, OpenAIStreamEvent, OpenAIError>
    private lateinit var testDataChunks: List<String>

    @BeforeEach
    fun setUp() {
        sourceProvider = OpenAIProvider(config = OpenAIProviderConfig.default())
        targetProvider = OpenAIProvider(config = OpenAIProviderConfig.default())
        kolo = Kolo(sourceProvider, targetProvider)

        // Load test data chunks
        testDataChunks = loadTestDataChunks()
    }

    @Test
    fun `should verify test setup works`() {
        assertThat(kolo).isNotNull()
        assertThat(testDataChunks).isNotEmpty()

        println("Test data chunks loaded: ${testDataChunks.size}")
        if (testDataChunks.isNotEmpty()) {
            println("First chunk: ${testDataChunks.first().take(100)}...")
        }
    }

    @Test
    fun `should perform end-to-end OpenAI to OpenAI conversion with streaming`() = runBlocking<Unit> {
        // Given
        val originalRequest = OpenAIRequest(
            model = "gpt-4o-mini-2024-07-18",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Tell me a story about a young girl named Lila"),
            ),
            temperature = 0.7,
            maxTokens = 500,
            stream = true,
        )

        // When - Convert request through Kolo
        val convertedRequest = kolo.convertSourceRequestToTarget(originalRequest)

        // Then - Verify request conversion
        assertThat(convertedRequest).isNotNull()
        assertThat(convertedRequest.model).isEqualTo(originalRequest.model)
        assertThat(convertedRequest.messages).hasSize(1)
        assertThat(convertedRequest.messages[0].role).isEqualTo("user")
        assertThat(convertedRequest.messages[0].content).isEqualTo("Tell me a story about a young girl named Lila")
        assertThat(convertedRequest.temperature).isEqualTo(0.7)
        assertThat(convertedRequest.maxTokens).isEqualTo(500)
        assertThat(convertedRequest.stream).isTrue()
    }

    @Test
    fun `should process streaming data with random sequential chunks`() = runBlocking<Unit> {
        // Given
        val mockStreamingData = createMockStreamingData()

        // When - Process streaming data through source provider
        val processedStream = sourceProvider.processStreamingData(mockStreamingData)
        val streamEvents = processedStream.toList()

        // Then - Verify streaming processing
        assertThat(streamEvents).isNotEmpty()
        assertThat(streamEvents.size).isGreaterThan(0)

        // Verify that we have content delta events
        val contentEvents = streamEvents.filterIsInstance<com.fatihcure.kolo.core.IntermittentStreamEvent.MessageDelta>()
        assertThat(contentEvents).isNotEmpty()

        // Verify that content is being accumulated correctly
        val fullContent = contentEvents.joinToString("") { it.delta.content ?: "" }
        assertThat(fullContent).isNotEmpty()
        assertThat(fullContent).contains("Lila") // Should contain parts of the story
    }

    @Test
    fun `should handle complete streaming conversion flow`() = runBlocking<Unit> {
        // Given
        val originalRequest = OpenAIRequest(
            model = "gpt-4o-mini-2024-07-18",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Write a short story"),
            ),
            temperature = 0.7,
            stream = true,
        )

        val mockStreamingData = createMockStreamingData()

        // When - Complete conversion flow
        val convertedRequest = kolo.convertSourceRequestToTarget(originalRequest)
        val processedStream = sourceProvider.processStreamingData(mockStreamingData)
        val transformedStream = sourceProvider.processStreamingDataToStreamEvent(processedStream)
        val streamEvents = transformedStream.toList()

        // Then - Verify complete flow
        assertThat(convertedRequest).isNotNull()
        assertThat(streamEvents).isNotEmpty()

        // Verify stream events have proper structure
        streamEvents.forEach { event ->
            assertThat(event.id).isNotNull()
            assertThat(event.model).isNotNull()
            assertThat(event.choices).isNotNull()
        }

        // Verify we have both content and finish events
        val hasContentEvents = streamEvents.any { it.choices?.any { choice -> choice.delta?.content != null } == true }
        val hasFinishEvents = streamEvents.any { it.choices?.any { choice -> choice.finishReason != null } == true }

        assertThat(hasContentEvents).isTrue()
        assertThat(hasFinishEvents).isTrue()
    }

    @Test
    fun `should handle streaming with random chunk sizes`() = runBlocking<Unit> {
        // Given - Create streaming data with random chunk sizes
        val randomChunkSizes = listOf(1, 3, 2, 5, 1, 4, 2, 3) // Random sequence of chunk sizes
        val mockStreamingData = createMockStreamingDataWithRandomChunks(randomChunkSizes)

        // When - Process through Kolo
        val streamEvents = kolo.processRawStreamingToSourceStreaming(mockStreamingData).toList()

        // Then
        println("Stream events count: ${streamEvents.size}")
        if (streamEvents.isNotEmpty()) {
            println("First event: ${streamEvents.first()}")
        }
        assertThat(streamEvents).isNotEmpty()

        // Verify that all events are properly processed regardless of chunk size
        streamEvents.forEach { event ->
            assertThat(event.id).isNotNull()
            assertThat(event.model).isNotNull()
            assertThat(event.choices).isNotNull()
        }
    }

    @Test
    fun `should handle streaming with partial chunks`() = runBlocking<Unit> {
        // Given - Create streaming data that includes partial chunks
        val mockStreamingData = createMockStreamingDataWithPartialChunks()

        // When - Process through Kolo
        val streamEvents = kolo.processRawStreamingToSourceStreaming(mockStreamingData).toList()

        // Then
        assertThat(streamEvents).isNotEmpty()

        // Verify that partial chunks are handled correctly
        val contentEvents = streamEvents.filter { it.choices?.any { choice -> choice.delta?.content != null } == true }
        assertThat(contentEvents).isNotEmpty()
    }

    @Test
    fun `should demonstrate full conversion pipeline through Kolo`() = runBlocking<Unit> {
        // Given
        val originalRequest = OpenAIRequest(
            model = "gpt-4o-mini-2024-07-18",
            messages = listOf(
                OpenAIMessage(role = "user", content = "Tell me a story about a young girl named Lila"),
            ),
            temperature = 0.7,
            maxTokens = 500,
            stream = true,
        )

        val mockStreamingData = createMockStreamingData()

        // When - Full conversion pipeline using Kolo
        val convertedRequest = kolo.convertSourceRequestToTarget(originalRequest)
        val sourceStreamEvents = kolo.processRawStreamingToSourceStreaming(mockStreamingData).toList()
        val targetStreamEvents = kolo.processRawStreamingToTargetStreaming(mockStreamingData).toList()
        val conversionStreamEvents = kolo.processRawStreamingThroughConversion(mockStreamingData).toList()

        // Then - Verify all conversion paths work
        assertThat(convertedRequest).isNotNull()
        assertThat(convertedRequest.model).isEqualTo(originalRequest.model)
        assertThat(convertedRequest.messages).hasSize(1)
        assertThat(convertedRequest.messages[0].content).isEqualTo("Tell me a story about a young girl named Lila")

        // Verify streaming processing works
        assertThat(sourceStreamEvents).isNotEmpty()
        assertThat(targetStreamEvents).isNotEmpty()
        assertThat(conversionStreamEvents).isNotEmpty()

        // Verify all streams have proper structure
        listOf(sourceStreamEvents, targetStreamEvents, conversionStreamEvents).forEach { events ->
            events.forEach { event ->
                assertThat(event.id).isNotNull()
                assertThat(event.model).isNotNull()
                assertThat(event.choices).isNotNull()
            }
        }

        // Verify content is being processed
        val hasContent = sourceStreamEvents.any { it.choices?.any { choice -> choice.delta?.content != null } == true }
        assertThat(hasContent).isTrue()
    }

    /**
     * Load test data chunks from test_data.json file
     * The file is already in SSE format with 'data:' prefix
     */
    private fun loadTestDataChunks(): List<String> {
        val testDataFile = File("test_data.json")
        return if (testDataFile.exists()) {
            testDataFile.readLines()
                .filter { it.startsWith("data: ") && !it.contains("[DONE]") }
                .map { it } // Keep the full SSE format including "data: " prefix
        } else {
            // Fallback test data if file doesn't exist
            listOf(
                """data: {"id":"test-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}""",
                """data: {"id":"test-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}""",
                """data: {"id":"test-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" world"},"logprobs":null,"finish_reason":null}]}""",
                """data: {"id":"test-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"stop"}]}""",
            )
        }
    }

    /**
     * Create mock streaming data with random sequential chunks from test data
     */
    private fun createMockStreamingData(): Flow<String> = flow {
        testDataChunks.forEach { chunk ->
            emit("$chunk\n\n") // Chunk already has "data: " prefix
            delay(10) // Simulate network delay
        }

        emit("data: [DONE]\n\n")
    }

    /**
     * Create mock streaming data with specific chunk sizes
     */
    private fun createMockStreamingDataWithRandomChunks(chunkSizes: List<Int>): Flow<String> = flow {
        var currentIndex = 0

        chunkSizes.forEach { chunkSize ->
            val chunks = testDataChunks.subList(
                currentIndex,
                minOf(currentIndex + chunkSize, testDataChunks.size),
            )

            chunks.forEach { chunk ->
                emit("$chunk\n\n") // Chunk already has "data: " prefix
                delay(5) // Simulate network delay
            }

            currentIndex += chunkSize
            if (currentIndex >= testDataChunks.size) return@flow
        }

        emit("data: [DONE]\n\n")
    }

    /**
     * Create mock streaming data with partial chunks to test buffering
     */
    private fun createMockStreamingDataWithPartialChunks(): Flow<String> = flow {
        // Emit a complete chunk
        val completeChunk = testDataChunks.first()
        emit("$completeChunk\n\n") // Chunk already has "data: " prefix
        delay(10)

        // Emit a partial chunk (missing closing brace and newlines)
        val partialChunk = testDataChunks[1].substring(0, testDataChunks[1].length - 10)
        emit(partialChunk) // Remove "data: " prefix for partial chunk
        delay(10)

        // Complete the partial chunk
        val completion = testDataChunks[1].substring(testDataChunks[1].length - 10)
        emit("$completion\n\n")
        delay(10)

        // Emit a few more complete chunks
        testDataChunks.drop(2).take(5).forEach { chunk ->
            emit("$chunk\n\n") // Chunk already has "data: " prefix
            delay(10)
        }

        emit("data: [DONE]\n\n")
    }
}
