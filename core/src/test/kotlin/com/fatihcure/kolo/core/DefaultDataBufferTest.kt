package com.fatihcure.kolo.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultDataBufferTest {

    private lateinit var buffer: DefaultDataBuffer

    @BeforeEach
    fun setUp() {
        buffer = DefaultDataBuffer()
    }

    @Test
    fun `should add chunk and return complete chunks`() {
        val data = "data: {\"content\": \"Hello\"}\n\n"
        val result = buffer.addChunk(data)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo("{\"content\": \"Hello\"}")
    }

    @Test
    fun `should handle partial chunks correctly`() {
        val partialData1 = "data: {\"content\": \"Hello"
        val partialData2 = " World\"}\n\n"

        val result1 = buffer.addChunk(partialData1)
        assertThat(result1).isEmpty()

        val result2 = buffer.addChunk(partialData2)
        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo("{\"content\": \"Hello World\"}")
    }

    @Test
    fun `should handle multiple complete chunks in one add`() {
        val data = "data: {\"content\": \"First\"}\n\ndata: {\"content\": \"Second\"}\n\n"
        val result = buffer.addChunk(data)

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo("{\"content\": \"First\"}")
        assertThat(result[1]).isEqualTo("{\"content\": \"Second\"}")
    }

    @Test
    fun `should ignore DONE chunks`() {
        val data = "data: [DONE]\n\n"
        val result = buffer.addChunk(data)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should get remaining data when buffer has incomplete JSON`() {
        val data = "data: {\"content\": \"Hello\""
        buffer.addChunk(data)

        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull() // Incomplete JSON
    }

    @Test
    fun `should get remaining data when buffer has complete JSON`() {
        val data = "data: {\"content\": \"Hello\"}"
        buffer.addChunk(data)

        val remaining = buffer.getRemainingData()
        assertThat(remaining).isEqualTo("{\"content\": \"Hello\"}")
    }

    @Test
    fun `should clear buffer`() {
        val data = "data: {\"content\": \"Hello\"}\n\n"
        buffer.addChunk(data)

        buffer.clear()
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull()
    }

    @Test
    fun `should throw BufferSizeExceededException when adding data exceeds max buffer size`() {
        val config = StreamingConfig(maxBufferSize = 10)
        val smallBuffer = DefaultDataBuffer(config)

        // Add data that would exceed the buffer size
        val largeData = "data: {\"content\": \"This is a very long message that exceeds the buffer size\"}\n\n"

        assertThatThrownBy { smallBuffer.addChunk(largeData) }
            .isInstanceOf(BufferSizeExceededException::class.java)
            .hasMessageContaining("Buffer size exceeded")
            .hasMessageContaining("current size 0")
            .hasMessageContaining("additional data ${largeData.length}")
            .hasMessageContaining("max size 10")
    }

    @Test
    fun `should throw BufferSizeExceededException when buffer already has data and adding more exceeds limit`() {
        val config = StreamingConfig(maxBufferSize = 50)
        val smallBuffer = DefaultDataBuffer(config)

        // Add some data first
        val firstData = "data: {\"test\": \"a\"}\n\n"
        smallBuffer.addChunk(firstData)

        // Now add data that would exceed the remaining buffer space
        val largeData = "data: {\"content\": \"This is a very long message that exceeds the buffer\"}\n\n"

        assertThatThrownBy { smallBuffer.addChunk(largeData) }
            .isInstanceOf(BufferSizeExceededException::class.java)
            .hasMessageContaining("Buffer size exceeded")
            .hasMessageContaining("max size 50")
    }

    @Test
    fun `should not throw exception when data fits within buffer size`() {
        val config = StreamingConfig(maxBufferSize = 100)
        val buffer = DefaultDataBuffer(config)

        val data = "data: {\"content\": \"Hello World\"}\n\n"

        // Should not throw any exception
        val result = buffer.addChunk(data)
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo("{\"content\": \"Hello World\"}")
    }

    @Test
    fun `should throw exception with correct size information`() {
        val config = StreamingConfig(maxBufferSize = 20)
        val buffer = DefaultDataBuffer(config)

        // Add some data that doesn't complete a chunk to keep it in buffer
        val firstData = "data: {\"a\":1}\n" // No closing \n\n, so it stays in buffer
        buffer.addChunk(firstData)

        val secondData = "data: {\"b\":2}\n\n"

        assertThatThrownBy { buffer.addChunk(secondData) }
            .isInstanceOf(BufferSizeExceededException::class.java)
            .hasMessageContaining("Buffer size exceeded")
            .hasMessageContaining("max size 20")
    }

    @Test
    fun `should handle custom chunk separator`() {
        val config = StreamingConfig(chunkSeparator = "|||")
        val buffer = DefaultDataBuffer(config)

        val data = "data: {\"content\": \"Hello\"}|||"
        val result = buffer.addChunk(data)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo("{\"content\": \"Hello\"}|||")
    }

    @Test
    fun `should handle mixed data and non-data lines`() {
        val data = """event: message
data: {"content": "Hello"}

event: message
data: {"content": "World"}

"""

        val result = buffer.addChunk(data)

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo("{\"content\": \"Hello\"}")
        assertThat(result[1]).isEqualTo("{\"content\": \"World\"}")
    }

    @Test
    fun `should clean buffer after processing complete chunks`() {
        val data = "data: {\"content\": \"Hello\"}\n\n"
        val result = buffer.addChunk(data)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo("{\"content\": \"Hello\"}")

        // Buffer should be empty after processing complete chunk
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull()
    }

    @Test
    fun `should clean buffer after processing multiple complete chunks`() {
        val data = "data: {\"content\": \"First\"}\n\ndata: {\"content\": \"Second\"}\n\n"
        val result = buffer.addChunk(data)

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo("{\"content\": \"First\"}")
        assertThat(result[1]).isEqualTo("{\"content\": \"Second\"}")

        // Buffer should be empty after processing all complete chunks
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull()
    }

    @Test
    fun `should keep partial data in buffer when chunk is incomplete`() {
        val partialData = "data: {\"content\": \"Hello\"}\n" // Missing final \n
        val result = buffer.addChunk(partialData)

        assertThat(result).isEmpty() // No complete chunks yet

        // Buffer should contain the partial data, and getRemainingData returns the JSON part
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isEqualTo("{\"content\": \"Hello\"}") // Returns the clean JSON part
    }

    @Test
    fun `should clean processed data and keep remaining partial data`() {
        // Add complete chunk followed by partial data
        val data = "data: {\"content\": \"Complete\"}\n\ndata: {\"content\": \"Partial\"}\n"
        val result = buffer.addChunk(data)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo("{\"content\": \"Complete\"}")

        // Buffer should contain only the partial data, and getRemainingData returns the JSON part
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isEqualTo("{\"content\": \"Partial\"}") // Returns the clean JSON part
    }

    @Test
    fun `should clean buffer completely when all data is processed`() {
        val data1 = "data: {\"content\": \"First\"}\n\n"
        val data2 = "data: {\"content\": \"Second\"}\n\n"

        val result1 = buffer.addChunk(data1)
        val result2 = buffer.addChunk(data2)

        assertThat(result1).hasSize(1)
        assertThat(result1[0]).isEqualTo("{\"content\": \"First\"}")
        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo("{\"content\": \"Second\"}")

        // Buffer should be empty after each complete chunk
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull()
    }

    @Test
    fun `should clean buffer after processing DONE chunks`() {
        val data = "data: [DONE]\n\n"
        val result = buffer.addChunk(data)

        assertThat(result).isEmpty() // DONE chunks are not returned

        // Buffer should be empty after processing DONE chunk
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull()
    }

    @Test
    fun `should clean buffer after processing mixed complete and DONE chunks`() {
        val data = "data: {\"content\": \"Hello\"}\n\ndata: [DONE]\n\ndata: {\"content\": \"World\"}\n\n"
        val result = buffer.addChunk(data)

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo("{\"content\": \"Hello\"}")
        assertThat(result[1]).isEqualTo("{\"content\": \"World\"}")

        // Buffer should be empty after processing all chunks
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull()
    }

    @Test
    fun `should demonstrate buffer cleaning by completing partial data`() {
        // Add partial data first
        val partialData = "data: {\"content\": \"Hello\"}\n"
        val result1 = buffer.addChunk(partialData)
        assertThat(result1).isEmpty() // No complete chunks yet

        // Complete the chunk
        val completionData = "\n"
        val result2 = buffer.addChunk(completionData)
        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo("{\"content\": \"Hello\"}")

        // Buffer should be empty after processing complete chunk
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isNull()
    }

    @Test
    fun `should clean buffer after processing complete JSON in getRemainingData`() {
        // Add complete JSON data without chunk separator
        val completeJsonData = "data: {\"content\": \"Complete JSON\"}"
        val result = buffer.addChunk(completeJsonData)

        assertThat(result).isEmpty() // No chunk separator, so no complete chunks

        // getRemainingData should return the complete JSON
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isEqualTo("{\"content\": \"Complete JSON\"}")

        // After getting remaining data, buffer should still contain it
        val remaining2 = buffer.getRemainingData()
        assertThat(remaining2).isEqualTo("{\"content\": \"Complete JSON\"}")
    }

    @Test
    fun `should handle three chunk scenario correctly`() {
        // Scenario:
        // 1. First chunk: incomplete, buffered only
        // 2. Second chunk: completes first event + starts second event (partial)
        // 3. Third chunk: completes second event

        // First chunk - incomplete
        val chunk1 = "data: {\"content\": \"First event\"}\n"
        val result1 = buffer.addChunk(chunk1)
        assertThat(result1).isEmpty() // No complete chunks yet
        assertThat(buffer.getRemainingData()).isEqualTo("{\"content\": \"First event\"}")

        // Second chunk - completes first event and starts second event
        val chunk2 = "\ndata: {\"content\": \"Second event\"}\n"
        val result2 = buffer.addChunk(chunk2)
        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo("{\"content\": \"First event\"}")
        // Buffer should now contain only the partial second event
        assertThat(buffer.getRemainingData()).isEqualTo("{\"content\": \"Second event\"}")

        // Third chunk - completes second event
        val chunk3 = "\n"
        val result3 = buffer.addChunk(chunk3)
        assertThat(result3).hasSize(1)
        assertThat(result3[0]).isEqualTo("{\"content\": \"Second event\"}")
        // Buffer should be empty after processing complete chunk
        assertThat(buffer.getRemainingData()).isNull()
    }

    @Test
    fun `should handle complex three chunk scenario with detailed verification`() {
        // More detailed test to verify the exact behavior described by user
        // This test verifies that partial data is properly preserved in buffer

        // First chunk - incomplete JSON (missing closing brace and separator)
        val chunk1 = "data: {\"content\": \"First event\""
        val result1 = buffer.addChunk(chunk1)
        assertThat(result1).isEmpty() // No complete chunks yet
        assertThat(buffer.getRemainingData()).isNull() // Incomplete JSON, not returned

        // Second chunk - completes first event and starts second event (partial)
        val chunk2 = "}\n\ndata: {\"content\": \"Second event\""
        val result2 = buffer.addChunk(chunk2)
        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo("{\"content\": \"First event\"}")
        // Buffer should now contain only the partial second event
        assertThat(buffer.getRemainingData()).isNull() // Incomplete JSON, not returned

        // Third chunk - completes second event
        val chunk3 = "}\n\n"
        val result3 = buffer.addChunk(chunk3)
        assertThat(result3).hasSize(1)
        assertThat(result3[0]).isEqualTo("{\"content\": \"Second event\"}")
        // Buffer should be empty after processing complete chunk
        assertThat(buffer.getRemainingData()).isNull()
    }

    @Test
    fun `should demonstrate potential buffer issue with exact scenario`() {
        // This test demonstrates the exact scenario described by the user
        // where the buffer might not properly handle partial data preservation

        // Step 1: First chunk, not complete and buffered only
        val chunk1 = "data: {\"id\": 1, \"content\": \"First\"}\n"
        val result1 = buffer.addChunk(chunk1)
        assertThat(result1).isEmpty() // No complete chunks yet
        // Buffer should contain: "data: {\"id\": 1, \"content\": \"First\"}\n"
        assertThat(buffer.getRemainingData()).isEqualTo("{\"id\": 1, \"content\": \"First\"}")

        // Step 2: Second chunk, completes first event and adds not completed part for second event
        val chunk2 = "\ndata: {\"id\": 2, \"content\": \"Second\"}\n"
        val result2 = buffer.addChunk(chunk2)
        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo("{\"id\": 1, \"content\": \"First\"}")
        // Buffer should now contain only: "data: {\"id\": 2, \"content\": \"Second\"}\n"
        // (the partial second event)
        assertThat(buffer.getRemainingData()).isEqualTo("{\"id\": 2, \"content\": \"Second\"}")

        // Step 3: Third chunk, completes the second event
        val chunk3 = "\n"
        val result3 = buffer.addChunk(chunk3)
        assertThat(result3).hasSize(1)
        assertThat(result3[0]).isEqualTo("{\"id\": 2, \"content\": \"Second\"}")
        // Buffer should be empty after processing complete chunk
        assertThat(buffer.getRemainingData()).isNull()
    }

    @Test
    fun `should handle case where partial data has trailing newline`() {
        // This test checks if the buffer properly handles partial data with trailing newlines
        // The getRemainingData should return clean JSON without trailing characters

        // Add partial data with trailing newline
        val partialData = "data: {\"content\": \"Partial\"}\n"
        val result = buffer.addChunk(partialData)

        assertThat(result).isEmpty() // No complete chunks yet
        // The getRemainingData should return the JSON part without the trailing newline
        val remaining = buffer.getRemainingData()
        assertThat(remaining).isEqualTo("{\"content\": \"Partial\"}") // Clean JSON without trailing \n
    }

    @Test
    fun `should handle multi-line SSE data correctly`() {
        // Test that multiple data: lines are properly concatenated according to SSE specification
        val multiLineData = """data: {"content": "First line"}
data: {"content": "Second line"}
data: {"content": "Third line"}

"""
        val result = buffer.addChunk(multiLineData)

        assertThat(result).hasSize(1)
        // All data lines should be concatenated with newlines
        assertThat(result[0]).isEqualTo(
            """{"content": "First line"}
{"content": "Second line"}
{"content": "Third line"}""",
        )
    }

    @Test
    fun `should handle mixed single and multi-line SSE data`() {
        // Test handling of both single-line and multi-line SSE events
        val mixedData = """data: {"single": "event"}

data: {"multi": "line1"}
data: {"multi": "line2"}

data: {"another": "single"}

"""
        val result = buffer.addChunk(mixedData)

        assertThat(result).hasSize(3)
        assertThat(result[0]).isEqualTo("""{"single": "event"}""")
        assertThat(result[1]).isEqualTo(
            """{"multi": "line1"}
{"multi": "line2"}""",
        )
        assertThat(result[2]).isEqualTo("""{"another": "single"}""")
    }
}
