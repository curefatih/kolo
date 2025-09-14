package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.DefaultDataBuffer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAIStreamingTest {

    @Test
    fun `test streaming with complete chunks`() = runBlocking {
        val provider = OpenAIProvider(config = OpenAIProviderConfig.default())

        val completeChunk = """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

"""

        val stream = flowOf(completeChunk)
        val result = provider.processStreamingDataToStreamingResponse(stream).toList()

        assertTrue(result.isNotEmpty())
        assertEquals("chatcmpl-123", result.first().id)
        assertEquals("gpt-4o-mini", result.first().model)
    }

    @Test
    fun `test streaming with partial chunks`() = runBlocking {
        val provider = OpenAIProvider(config = OpenAIProviderConfig.default())

        val partialChunk1 = """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

"""

        val partialChunk2 = """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}

"""

        val stream = flowOf(partialChunk1, partialChunk2)
        val result = provider.processStreamingDataToStreamingResponse(stream).toList()

        assertTrue(result.isNotEmpty())
        assertEquals(2, result.size)
    }

    @Test
    fun `test streaming with incomplete chunk`() = runBlocking {
        val provider = OpenAIProvider(config = OpenAIProviderConfig.default())

        // Truly incomplete chunk - missing closing braces and \n\n
        val incompleteChunk = """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"role":"assistant","content":"","refusal":null"""

        val stream = flowOf(incompleteChunk)
        val result = provider.processStreamingDataToStreamingResponse(stream).toList()

        // Should not emit anything for incomplete chunks
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test streaming with custom data buffer`() = runBlocking {
        val provider = OpenAIProvider(
            OpenAIProviderConfig.withDataBufferFactory(object : com.fatihcure.kolo.core.DataBufferFactory {
                override fun createBuffer(config: com.fatihcure.kolo.core.StreamingConfig): com.fatihcure.kolo.core.DataBuffer = DefaultDataBuffer(config)
            }),
        )

        val completeChunk = """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"role":"assistant","content":"","refusal":null},"logprobs":null,"finish_reason":null}]}

"""

        val stream = flowOf(completeChunk)
        val result = provider.processStreamingDataToStreamingResponse(stream).toList()

        // Debug output
        println("Result size: ${result.size}")
        result.forEachIndexed { index, item ->
            println("Result[$index]: $item")
        }

        assertTrue(result.isNotEmpty())
        assertEquals("chatcmpl-123", result.first().id)
    }

    @Test
    fun `test data buffer with partial chunks`() = runBlocking {
        val buffer = DefaultDataBuffer(com.fatihcure.kolo.core.StreamingConfig())

        // Test truly incomplete chunk (missing closing brace and newlines)
        val partialChunk = """data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[{"index":0,"delta":{"role":"assistant","content":"","refusal":null},"logprobs":null,"finish_reason":null}],"usage":null,"obfuscation":"AAatV4uAp"
"""

        val result1 = buffer.addChunk(partialChunk)
        assertTrue(result1.isEmpty()) // Should not return anything for incomplete chunk

        // Test completion of chunk
        val completionChunk = """}

"""

        val result2 = buffer.addChunk(completionChunk)
        assertTrue(result2.isNotEmpty()) // Should return complete chunk
    }

    @Test
    fun `test streaming with real OpenAI format`() = runBlocking {
        val provider = OpenAIProvider(config = OpenAIProviderConfig.default())

        // Test with the actual format provided by the user
        val streamData = flowOf(
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini", "system_fingerprint": "fp_447092222", "choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini", "system_fingerprint": "fp_447092222", "choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}

""",
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini", "system_fingerprint": "fp_447092222", "choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"stop"}]}

""",
        )

        val result = provider.processStreamingDataToStreamingResponse(streamData).toList()

        assertTrue(result.isNotEmpty())
        assertEquals(3, result.size)

        // Check first chunk (role assignment)
        val firstChunk = result[0]
        assertEquals("chatcmpl-123", firstChunk.id)
        assertEquals("gpt-4o-mini", firstChunk.model)
        assertEquals("assistant", firstChunk.choices?.first()?.delta?.role)
        assertEquals("", firstChunk.choices?.first()?.delta?.content)

        // Check second chunk (content)
        val secondChunk = result[1]
        assertEquals("Hello", secondChunk.choices?.first()?.delta?.content)

        // Check third chunk (finish)
        val thirdChunk = result[2]
        assertEquals("stop", thirdChunk.choices?.first()?.finishReason)
    }

    @Test
    fun `test streaming with custom data buffer factory`() = runBlocking {
        val customFactory = object : com.fatihcure.kolo.core.DataBufferFactory {
            override fun createBuffer(config: com.fatihcure.kolo.core.StreamingConfig): com.fatihcure.kolo.core.DataBuffer = DefaultDataBuffer(config)
        }
        val provider = OpenAIProvider(OpenAIProviderConfig.withDataBufferFactory(customFactory))

        val completeChunk = """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","system_fingerprint":"fp_447092222","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

"""

        val stream = flowOf(completeChunk)
        val result = provider.processStreamingDataToStreamingResponse(stream).toList()

        assertTrue(result.isNotEmpty())
        assertEquals("chatcmpl-123", result.first().id)
    }

    @Test
    fun `test streaming with real OpenAI events including obfuscation and usage`() = runBlocking {
        val provider = OpenAIProvider(config = OpenAIProviderConfig.default())

        // Test with the actual format provided by the user - last 4 streaming events
        val streamData = flowOf(
            """data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[{"index":0,"delta":{"content":" them"},"logprobs":null,"finish_reason":null}],"usage":null,"obfuscation":"lFQou2"}

""",
            """data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[{"index":0,"delta":{"content":"."},"logprobs":null,"finish_reason":null}],"usage":null,"obfuscation":"ITgg9cYU7y"}

""",
            """data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"stop"}],"usage":null,"obfuscation":"8shbK"}

""",
            """data: {"id":"chatcmpl-C42qMU7","object":"chat.completion.chunk","created":1755080278,"model":"gpt-4o-mini-2024-07-18","service_tier":"default","system_fingerprint":"fp_34a54111","choices":[],"usage":{"prompt_tokens":13,"completion_tokens":567,"total_tokens":580,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}},"obfuscation":"QfwgjUV1"}

""",
            """data: [DONE]

""",
        )

        val result = provider.processStreamingDataToStreamingResponse(streamData).toList()

        assertTrue(result.isNotEmpty())
        assertEquals(4, result.size) // Should have 4 events, not including [DONE]

        // Check first chunk (content delta)
        val firstChunk = result[0]
        assertEquals("chatcmpl-C42qMU7", firstChunk.id)
        assertEquals("gpt-4o-mini-2024-07-18", firstChunk.model)
        assertEquals(" them", firstChunk.choices?.first()?.delta?.content)
        assertEquals(null, firstChunk.choices?.first()?.finishReason)

        // Check second chunk (content delta)
        val secondChunk = result[1]
        assertEquals(".", secondChunk.choices?.first()?.delta?.content)
        assertEquals(null, secondChunk.choices?.first()?.finishReason)

        // Check third chunk (finish)
        val thirdChunk = result[2]
        assertEquals("stop", thirdChunk.choices?.first()?.finishReason)
        assertEquals(null, thirdChunk.choices?.first()?.delta?.content)

        // Check fourth chunk (usage)
        val fourthChunk = result[3]
        assertEquals(13, fourthChunk.usage?.promptTokens)
        assertEquals(567, fourthChunk.usage?.completionTokens)
        assertEquals(580, fourthChunk.usage?.totalTokens)
        assertTrue(fourthChunk.choices?.isEmpty() == true)
    }
}
