package com.fatihcure.kolo.core

/**
 * Default implementation of DataBuffer for handling streaming data
 * This implementation handles Server-Sent Events (SSE) format with "data: " prefix
 */
class DefaultDataBuffer(
    config: StreamingConfig = StreamingConfig(),
) : AbstractDataBuffer(config) {

    override fun addChunk(data: String): List<String> {
        // Check if adding this data would exceed the maximum buffer size
        if (buffer.length + data.length > config.maxBufferSize) {
            // If buffer is already at or near max size, clear it to prevent memory issues
            buffer.clear()
        }

        buffer.append(data)
        val completeChunks = mutableListOf<String>()

        val chunkSeparator = getChunkSeparator()
        var currentIndex = 0
        while (currentIndex < buffer.length) {
            val nextSeparator = buffer.indexOf(chunkSeparator, currentIndex)
            if (nextSeparator != -1) {
                val chunk = buffer.substring(currentIndex, nextSeparator + chunkSeparator.length)
                val processedChunk = processChunk(chunk)
                if (processedChunk != null) {
                    completeChunks.add(processedChunk)
                }
                currentIndex = nextSeparator + chunkSeparator.length
            } else {
                break
            }
        }

        if (currentIndex > 0) {
            buffer.delete(0, currentIndex)
        }

        return completeChunks
    }

    override fun getRemainingData(): String? {
        return if (buffer.isNotEmpty()) {
            val remaining = buffer.toString()
            if (remaining.startsWith("data: ") && remaining.contains("{") && remaining.contains("}")) {
                val jsonPart = remaining.substring(6)
                val openBraces = jsonPart.count { it == '{' }
                val closeBraces = jsonPart.count { it == '}' }
                if (openBraces > 0 && openBraces == closeBraces) {
                    jsonPart
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    override fun clear() {
        buffer.clear()
    }

    /**
     * Process a complete chunk to extract the actual data
     * Handles SSE format: "data: {json}\n\n"
     */
    private fun processChunk(chunk: String): String? {
        val lines = chunk.trim().split("\n")
        for (line in lines) {
            if (line.startsWith("data: ")) {
                val data = line.substring(6)
                if (data.trim() == "[DONE]") {
                    return null
                }
                return data
            }
        }
        return null
    }
}
