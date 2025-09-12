package com.fatihcure.kolo.normalizers

import com.fatihcure.kolo.core.IntermittentMessage
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * Test utilities for normalizer testing
 */
object TestUtils {

    /**
     * Helper function to collect all items from a Flow for testing
     */
    suspend fun <T> collectAll(flow: Flow<T>): List<T> = flow.toList()

    /**
     * Helper function to run coroutine-based tests
     */
    fun <T> runTest(block: suspend () -> T): T = runBlocking { block() }

    /**
     * Create a sample IntermittentRequest for testing
     */
    fun createSampleIntermittentRequest(): IntermittentRequest {
        return IntermittentRequest(
            messages = listOf(
                IntermittentMessage(
                    role = MessageRole.SYSTEM,
                    content = "You are a helpful assistant.",
                ),
                IntermittentMessage(
                    role = MessageRole.USER,
                    content = "Hello, how are you?",
                ),
            ),
            model = "test-model",
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            stop = listOf("STOP"),
            stream = false,
        )
    }

    /**
     * Create a sample IntermittentResponse for testing
     */
    fun createSampleIntermittentResponse(): IntermittentResponse {
        return IntermittentResponse(
            id = "test-response-id",
            model = "test-model",
            choices = listOf(
                com.fatihcure.kolo.core.IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "I'm doing well, thank you!",
                    ),
                ),
            ),
            usage = com.fatihcure.kolo.core.IntermittentUsage(
                promptTokens = 10,
                completionTokens = 8,
                totalTokens = 18,
            ),
        )
    }

    /**
     * Create a sample streaming flow for testing
     */
    fun createSampleStreamingFlow(): Flow<IntermittentStreamEvent> {
        return flowOf(
            IntermittentStreamEvent.MessageStart(
                id = "stream-id",
                model = "test-model",
            ),
            IntermittentStreamEvent.MessageDelta(
                delta = com.fatihcure.kolo.core.IntermittentDelta(
                    content = "Hello",
                ),
            ),
            IntermittentStreamEvent.MessageDelta(
                delta = com.fatihcure.kolo.core.IntermittentDelta(
                    content = " there",
                ),
            ),
            IntermittentStreamEvent.MessageEnd(
                finishReason = "stop",
                usage = com.fatihcure.kolo.core.IntermittentUsage(
                    promptTokens = 10,
                    completionTokens = 2,
                    totalTokens = 12,
                ),
            ),
        )
    }
}
