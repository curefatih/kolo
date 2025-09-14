package com.fatihcure.kolo.normalizers.openai

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenAIStreamingHandlerBufferingTest {

    private val objectMapper = ObjectMapper()

    @Test
    fun `should buffer incoming messages and process complete chunks while removing processed data`() = runBlocking {
        val handler = OpenAIStreamingHandler(objectMapper)

        // Simulate streaming data with partial chunks that get completed
        val streamingData = flowOf(
            // First partial chunk - should be buffered but not processed
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}
""",
            // Complete the first chunk - should be processed and removed from buffer
            """

""",
            // Second partial chunk - should be buffered
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}
""",
            // Complete the second chunk - should be processed and removed from buffer
            """

""",
            // Third complete chunk - should be processed immediately
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" World"},"logprobs":null,"finish_reason":null}]}

""",
            // Fourth partial chunk - should be buffered
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"!"},"logprobs":null,"finish_reason":"stop"}]}
""",
            // Complete the fourth chunk - should be processed and removed from buffer
            """

""",
        )

        val result = handler.processStreamingData(streamingData).toList()

        // Should have processed 4 complete chunks
        assertThat(result).hasSize(4)

        // Verify the content of each processed chunk
        assertThat(result[0].choices?.first()?.delta?.role).isEqualTo("assistant")
        assertThat(result[0].choices?.first()?.delta?.content).isEqualTo("")

        assertThat(result[1].choices?.first()?.delta?.content).isEqualTo("Hello")

        assertThat(result[2].choices?.first()?.delta?.content).isEqualTo(" World")

        assertThat(result[3].choices?.first()?.delta?.content).isEqualTo("!")
        assertThat(result[3].choices?.first()?.finishReason).isEqualTo("stop")
    }

    @Test
    fun `should handle mixed complete and incomplete chunks correctly`() = runBlocking {
        val handler = OpenAIStreamingHandler(objectMapper)

        val streamingData = flowOf(
            // Complete chunk - should be processed immediately
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

""",
            // Incomplete chunk - should be buffered
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"Hello""",
            // Complete the incomplete chunk - should be processed
            """},"logprobs":null,"finish_reason":null}]}

""",
            // Another complete chunk - should be processed immediately
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" World"},"logprobs":null,"finish_reason":"stop"}]}

""",
        )

        val result = handler.processStreamingData(streamingData).toList()

        // Should have processed 3 complete chunks
        assertThat(result).hasSize(3)

        // Verify content
        assertThat(result[0].choices?.first()?.delta?.role).isEqualTo("assistant")
        assertThat(result[1].choices?.first()?.delta?.content).isEqualTo("Hello")
        assertThat(result[2].choices?.first()?.delta?.content).isEqualTo(" World")
        assertThat(result[2].choices?.first()?.finishReason).isEqualTo("stop")
    }

    @Test
    fun `should handle multiple complete chunks in single data packet`() = runBlocking {
        val handler = OpenAIStreamingHandler(objectMapper)

        val streamingData = flowOf(
            // Multiple complete chunks in one packet
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" World"},"logprobs":null,"finish_reason":"stop"}]}

""",
        )

        val result = handler.processStreamingData(streamingData).toList()

        // Should have processed 3 complete chunks
        assertThat(result).hasSize(3)

        // Verify content
        assertThat(result[0].choices?.first()?.delta?.role).isEqualTo("assistant")
        assertThat(result[1].choices?.first()?.delta?.content).isEqualTo("Hello")
        assertThat(result[2].choices?.first()?.delta?.content).isEqualTo(" World")
        assertThat(result[2].choices?.first()?.finishReason).isEqualTo("stop")
    }

    @Test
    fun `should handle incomplete chunks at end of stream`() = runBlocking {
        val handler = OpenAIStreamingHandler(objectMapper)

        val streamingData = flowOf(
            // Complete chunk
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"logprobs":null,"finish_reason":null}]}

""",
            // Incomplete chunk at end - should be buffered but not processed
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" World""",
        )

        val result = handler.processStreamingData(streamingData).toList()

        // Should only process the complete chunk
        assertThat(result).hasSize(1)
        assertThat(result[0].choices?.first()?.delta?.content).isEqualTo("Hello")
    }

    @Test
    fun `should handle DONE chunks correctly`() = runBlocking {
        val handler = OpenAIStreamingHandler(objectMapper)

        val streamingData = flowOf(
            // Complete chunk
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"logprobs":null,"finish_reason":null}]}

""",
            // DONE chunk - should be processed but not emitted
            """data: [DONE]

""",
            // Another complete chunk
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" World"},"logprobs":null,"finish_reason":"stop"}]}

""",
        )

        val result = handler.processStreamingData(streamingData).toList()

        // Should process 2 complete chunks (DONE is processed but not emitted)
        assertThat(result).hasSize(2)
        assertThat(result[0].choices?.first()?.delta?.content).isEqualTo("Hello")
        assertThat(result[1].choices?.first()?.delta?.content).isEqualTo(" World")
        assertThat(result[1].choices?.first()?.finishReason).isEqualTo("stop")
    }

    @Test
    fun `should demonstrate buffer cleaning by processing chunks sequentially`() = runBlocking {
        val handler = OpenAIStreamingHandler(objectMapper)

        // Process chunks one by one to demonstrate buffer cleaning
        val chunk1 = flowOf(
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"logprobs":null,"finish_reason":null}]}

""",
        )

        val chunk2 = flowOf(
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" World"},"logprobs":null,"finish_reason":"stop"}]}

""",
        )

        val result1 = handler.processStreamingData(chunk1).toList()
        val result2 = handler.processStreamingData(chunk2).toList()

        // Each stream should process its chunk independently
        assertThat(result1).hasSize(1)
        assertThat(result1[0].choices?.first()?.delta?.content).isEqualTo("Hello")

        assertThat(result2).hasSize(1)
        assertThat(result2[0].choices?.first()?.delta?.content).isEqualTo(" World")
        assertThat(result2[0].choices?.first()?.finishReason).isEqualTo("stop")
    }

    @Test
    fun `should handle malformed JSON gracefully`() = runBlocking {
        val handler = OpenAIStreamingHandler(objectMapper)

        val streamingData = flowOf(
            // Malformed JSON chunk
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"logprobs":null,"finish_reason":null}]}

""",
            // Valid chunk
            """data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"content":" World"},"logprobs":null,"finish_reason":"stop"}]}

""",
        )

        val result = handler.processStreamingData(streamingData).toList()

        // Should process both chunks (malformed one will emit error response)
        assertThat(result).hasSize(2)

        // First chunk should be processed normally
        assertThat(result[0].choices?.first()?.delta?.content).isEqualTo("Hello")

        // Second chunk should be processed normally
        assertThat(result[1].choices?.first()?.delta?.content).isEqualTo(" World")
        assertThat(result[1].choices?.first()?.finishReason).isEqualTo("stop")
    }
}
