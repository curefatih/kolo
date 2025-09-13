package com.fatihcure.kolo.core

/**
 * Default implementation of DataBuffer for handling streaming data
 * This implementation handles Server-Sent Events (SSE) format with "data: " prefix
 */
class DefaultDataBuffer : DataBuffer {
    private val buffer = StringBuilder()

    override fun addChunk(data: String): List<String> {
        buffer.append(data)
        val completeChunks = mutableListOf<String>()

        var currentIndex = 0
        while (currentIndex < buffer.length) {
            val nextDoubleNewline = buffer.indexOf("\n\n", currentIndex)
            if (nextDoubleNewline != -1) {
                val chunk = buffer.substring(currentIndex, nextDoubleNewline + 2)
                val processedChunk = processChunk(chunk)
                if (processedChunk != null) {
                    completeChunks.add(processedChunk)
                }
                currentIndex = nextDoubleNewline + 2
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
                if (openBraces == closeBraces) {
                    remaining
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
