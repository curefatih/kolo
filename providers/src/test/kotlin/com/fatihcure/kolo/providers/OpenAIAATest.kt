package com.fatihcure.kolo.providers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fatihcure.kolo.normalizers.openai.OpenAIChoice
import com.fatihcure.kolo.normalizers.openai.OpenAIMessage
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIUsage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A/A tests for OpenAI provider to verify that request/response conversion
 * is consistent and reversible (OpenAI -> Intermittent -> OpenAI)
 */
class OpenAIAATest {

    private val provider = OpenAIProvider(OpenAIProviderConfig.default())
    private val objectMapper = ObjectMapper()

    @Test
    fun `test request A-A conversion OpenAI to Intermittent to OpenAI`() {
        // Create original OpenAI request
        val originalRequest = OpenAIRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = "You are a helpful assistant.",
                ),
                OpenAIMessage(
                    role = "user",
                    content = "Hello, how are you?",
                ),
            ),
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            frequencyPenalty = 0.1,
            presencePenalty = 0.1,
            stop = listOf("STOP", "END"),
            stream = true,
        )

        // Convert to IntermittentRequest and back to OpenAIRequest
        val normalizedRequest = provider.normalizeRequest(originalRequest)
        val convertedRequest = provider.transformRequest(normalizedRequest)

        // Verify they are identical
        assertEquals(originalRequest.model, convertedRequest.model)
        assertEquals(originalRequest.temperature, convertedRequest.temperature)
        assertEquals(originalRequest.maxTokens, convertedRequest.maxTokens)
        assertEquals(originalRequest.topP, convertedRequest.topP)
        assertEquals(originalRequest.frequencyPenalty, convertedRequest.frequencyPenalty)
        assertEquals(originalRequest.presencePenalty, convertedRequest.presencePenalty)
        assertEquals(originalRequest.stop, convertedRequest.stop)
        assertEquals(originalRequest.stream, convertedRequest.stream)

        // Verify messages
        assertEquals(originalRequest.messages.size, convertedRequest.messages.size)
        for (i in originalRequest.messages.indices) {
            val original = originalRequest.messages[i]
            val converted = convertedRequest.messages[i]
            assertEquals(original.role, converted.role)
            assertEquals(original.content, converted.content)
            assertEquals(original.name, converted.name)
        }

        // Verify JSON serialization produces identical output
        val originalJson = objectMapper.writeValueAsString(originalRequest)
        val convertedJson = objectMapper.writeValueAsString(convertedRequest)
        assertEquals(originalJson, convertedJson, "JSON serialization should produce identical output")
    }

    @Test
    fun `test response A-A conversion OpenAI to Intermittent to OpenAI`() {
        // Create original OpenAI response
        val originalResponse = OpenAIResponse(
            id = "chatcmpl-123",
            model = "gpt-4o-mini",
            choices = listOf(
                OpenAIChoice(
                    index = 0,
                    message = OpenAIMessage(
                        role = "assistant",
                        content = "Hello! I'm doing well, thank you for asking.",
                    ),
                    finishReason = "stop",
                ),
            ),
            usage = OpenAIUsage(
                promptTokens = 20,
                completionTokens = 15,
                totalTokens = 35,
            ),
        )

        // Convert to IntermittentResponse and back to OpenAIResponse
        val normalizedResponse = provider.normalizeResponse(originalResponse)
        val convertedResponse = provider.transformResponse(normalizedResponse)

        // Verify they are identical
        assertEquals(originalResponse.id, convertedResponse.id)
        assertEquals(originalResponse.model, convertedResponse.model)

        // Verify choices
        assertEquals(originalResponse.choices.size, convertedResponse.choices.size)
        for (i in originalResponse.choices.indices) {
            val original = originalResponse.choices[i]
            val converted = convertedResponse.choices[i]
            assertEquals(original.index, converted.index)
            assertEquals(original.finishReason, converted.finishReason)

            // Verify message
            assertNotNull(original.message)
            assertNotNull(converted.message)
            assertEquals(original.message!!.role, converted.message!!.role)
            assertEquals(original.message!!.content, converted.message!!.content)
            assertEquals(original.message!!.name, converted.message!!.name)
        }

        // Verify usage
        assertNotNull(originalResponse.usage)
        assertNotNull(convertedResponse.usage)
        assertEquals(originalResponse.usage!!.promptTokens, convertedResponse.usage!!.promptTokens)
        assertEquals(originalResponse.usage!!.completionTokens, convertedResponse.usage!!.completionTokens)
        assertEquals(originalResponse.usage!!.totalTokens, convertedResponse.usage!!.totalTokens)

        // Verify JSON serialization produces identical output
        val originalJson = objectMapper.writeValueAsString(originalResponse)
        val convertedJson = objectMapper.writeValueAsString(convertedResponse)
        assertEquals(originalJson, convertedJson, "JSON serialization should produce identical output")
    }

    @Test
    fun `test error response A-A conversion`() {
        // Create original OpenAI error response
        val originalErrorResponse = OpenAIResponse(
            id = "",
            model = "",
            choices = emptyList(),
            usage = null,
        )

        // Convert to IntermittentError and back to OpenAIResponse
        val normalizedError = provider.normalizeError(originalErrorResponse)
        val convertedErrorResponse = provider.transformError(normalizedError)

        // Verify error conversion
        assertEquals(originalErrorResponse.id, convertedErrorResponse.id)
        assertEquals(originalErrorResponse.model, convertedErrorResponse.model)
        assertEquals(originalErrorResponse.choices.size, convertedErrorResponse.choices.size)
        assertEquals(originalErrorResponse.usage, convertedErrorResponse.usage)

        // Verify JSON serialization produces identical output
        val originalJson = objectMapper.writeValueAsString(originalErrorResponse)
        val convertedJson = objectMapper.writeValueAsString(convertedErrorResponse)
        assertEquals(originalJson, convertedJson, "JSON serialization should produce identical output")
    }

    @Test
    fun `test streaming response A-A conversion with raw data`() = runBlocking {
        // Test the complete flow: raw streaming data -> OpenAIStreamingResponse -> IntermittentStreamEvent -> OpenAIResponse
        val rawStreamingData = listOf(
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"stop"}]}

""",
            """data: [DONE]

""",
        )

        // Process through the complete flow
        val rawStream = flowOf(*rawStreamingData.toTypedArray())
        val streamingResponses = provider.processStreamingDataToStreamingResponse(rawStream).toList()

        // Verify we got responses
        assertTrue(streamingResponses.isNotEmpty())
        assertEquals(3, streamingResponses.size) // Should have 3 events, not including [DONE]

        // Verify the first response (role assignment)
        val firstResponse = streamingResponses[0]
        assertEquals("chatcmpl-123", firstResponse.id)
        assertEquals("gpt-4o-mini", firstResponse.model)
        assertNotNull(firstResponse.choices)
        assertEquals(1, firstResponse.choices!!.size)
        assertNotNull(firstResponse.choices!![0].delta)
        assertEquals("assistant", firstResponse.choices!![0].delta!!.role)
        assertEquals("", firstResponse.choices!![0].delta!!.content)

        // Verify the second response (content)
        val secondResponse = streamingResponses[1]
        assertEquals("Hello", secondResponse.choices!![0].delta!!.content)

        // Verify the third response (finish)
        val thirdResponse = streamingResponses[2]
        assertEquals("stop", thirdResponse.choices!![0].finishReason)

        // Verify JSON serialization produces valid output for each streaming response
        streamingResponses.forEachIndexed { index, response ->
            val json = objectMapper.writeValueAsString(response)
            assertTrue(json.isNotEmpty(), "Streaming response $index should serialize to non-empty JSON")
            // Verify we can deserialize it back
            val deserialized = objectMapper.readValue(json, com.fatihcure.kolo.normalizers.openai.OpenAIStreamingResponse::class.java)
            assertEquals(response.id, deserialized.id)
            assertEquals(response.model, deserialized.model)
        }
    }

    @Test
    fun `test streaming response processing with multiple chunks`() = runBlocking {
        // Test with multiple streaming responses to verify A/A conversion
        val streamingData = listOf(
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"content":" there!"},"logprobs":null,"finish_reason":null}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"stop"}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15},"obfuscation":"test"}

""",
            """data: [DONE]

""",
        )

        // Process through the complete flow
        val rawStream = flowOf(*streamingData.toTypedArray())
        val streamingResponses = provider.processStreamingDataToStreamingResponse(rawStream).toList()

        // Verify we got responses
        assertTrue(streamingResponses.isNotEmpty())
        assertEquals(5, streamingResponses.size) // Should have 5 events, not including [DONE]

        // Verify each response
        val firstResponse = streamingResponses[0]
        assertEquals("chatcmpl-123", firstResponse.id)
        assertEquals("assistant", firstResponse.choices!![0].delta!!.role)
        assertEquals("", firstResponse.choices!![0].delta!!.content)

        val secondResponse = streamingResponses[1]
        assertEquals("Hello", secondResponse.choices!![0].delta!!.content)

        val thirdResponse = streamingResponses[2]
        assertEquals(" there!", thirdResponse.choices!![0].delta!!.content)

        val fourthResponse = streamingResponses[3]
        assertEquals("stop", fourthResponse.choices!![0].finishReason)

        val fifthResponse = streamingResponses[4]
        assertEquals(10, fifthResponse.usage!!.promptTokens)
        assertEquals(5, fifthResponse.usage!!.completionTokens)
        assertEquals(15, fifthResponse.usage!!.totalTokens)
        assertTrue(fifthResponse.choices!!.isEmpty())

        // Verify JSON serialization produces valid output for each streaming response
        streamingResponses.forEachIndexed { index, response ->
            val json = objectMapper.writeValueAsString(response)
            assertTrue(json.isNotEmpty(), "Streaming response $index should serialize to non-empty JSON")
            // Verify we can deserialize it back
            val deserialized = objectMapper.readValue(json, com.fatihcure.kolo.normalizers.openai.OpenAIStreamingResponse::class.java)
            assertEquals(response.id, deserialized.id)
            assertEquals(response.model, deserialized.model)
            assertEquals(response.choices?.size ?: 0, deserialized.choices?.size ?: 0)
            if (response.usage != null) {
                assertNotNull(deserialized.usage)
                assertEquals(response.usage!!.promptTokens, deserialized.usage!!.promptTokens)
                assertEquals(response.usage!!.completionTokens, deserialized.usage!!.completionTokens)
                assertEquals(response.usage!!.totalTokens, deserialized.usage!!.totalTokens)
            }
        }
    }

    @Test
    fun `test JSON serialization A-A conversion`() {
        // Test that JSON serialization produces identical output for A/A conversion
        val originalRequest = OpenAIRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = "You are a helpful assistant.",
                ),
                OpenAIMessage(
                    role = "user",
                    content = "Hello, how are you?",
                ),
            ),
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            frequencyPenalty = 0.1,
            presencePenalty = 0.1,
            stop = listOf("STOP", "END"),
            stream = true,
        )

        // Convert to IntermittentRequest and back to OpenAIRequest
        val normalizedRequest = provider.normalizeRequest(originalRequest)
        val convertedRequest = provider.transformRequest(normalizedRequest)

        // Serialize both to JSON
        val originalJson = objectMapper.writeValueAsString(originalRequest)
        val convertedJson = objectMapper.writeValueAsString(convertedRequest)

        // The converted request should be identical to the original
        assertEquals(originalRequest.model, convertedRequest.model)
        assertEquals(originalRequest.temperature, convertedRequest.temperature)
        assertEquals(originalRequest.maxTokens, convertedRequest.maxTokens)
        assertEquals(originalRequest.topP, convertedRequest.topP)
        assertEquals(originalRequest.frequencyPenalty, convertedRequest.frequencyPenalty)
        assertEquals(originalRequest.presencePenalty, convertedRequest.presencePenalty)
        assertEquals(originalRequest.stop, convertedRequest.stop)
        assertEquals(originalRequest.stream, convertedRequest.stream)

        // JSON serialization should produce identical output
        assertEquals(originalJson, convertedJson, "JSON serialization should produce identical output")
    }

    @Test
    fun `test JSON serialization for response A-A conversion`() {
        // Test that JSON serialization produces identical output for A/A conversion
        val originalResponse = OpenAIResponse(
            id = "chatcmpl-123",
            model = "gpt-4o-mini",
            choices = listOf(
                OpenAIChoice(
                    index = 0,
                    message = OpenAIMessage(
                        role = "assistant",
                        content = "Hello! I'm doing well, thank you for asking.",
                    ),
                    finishReason = "stop",
                ),
            ),
            usage = OpenAIUsage(
                promptTokens = 20,
                completionTokens = 15,
                totalTokens = 35,
            ),
        )

        // Convert to IntermittentResponse and back to OpenAIResponse
        val normalizedResponse = provider.normalizeResponse(originalResponse)
        val convertedResponse = provider.transformResponse(normalizedResponse)

        // Serialize both to JSON
        val originalJson = objectMapper.writeValueAsString(originalResponse)
        val convertedJson = objectMapper.writeValueAsString(convertedResponse)

        // The converted response should be identical to the original
        assertEquals(originalResponse.id, convertedResponse.id)
        assertEquals(originalResponse.model, convertedResponse.model)
        assertEquals(originalResponse.choices.size, convertedResponse.choices.size)
        assertEquals(originalResponse.usage!!.promptTokens, convertedResponse.usage!!.promptTokens)
        assertEquals(originalResponse.usage!!.completionTokens, convertedResponse.usage!!.completionTokens)
        assertEquals(originalResponse.usage!!.totalTokens, convertedResponse.usage!!.totalTokens)

        // JSON serialization should produce identical output
        assertEquals(originalJson, convertedJson, "JSON serialization should produce identical output")
    }
}
