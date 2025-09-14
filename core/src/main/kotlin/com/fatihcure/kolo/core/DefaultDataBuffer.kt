package com.fatihcure.kolo.core

/**
 * Default implementation of DataBuffer for handling streaming data
 * This implementation handles Server-Sent Events (SSE) format with "data: " prefix
 */
class DefaultDataBuffer(
    config: StreamingConfig = StreamingConfig(),
) : AbstractDataBuffer(config) {

    override fun addChunk(data: String): List<String> {
        if (buffer.length + data.length > config.maxBufferSize) {
            throw BufferSizeExceededException(
                currentSize = buffer.length,
                maxSize = config.maxBufferSize,
                additionalDataSize = data.length,
            )
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
                    // Find the end of the JSON object and return only the JSON part
                    val jsonEndIndex = findJsonEndIndex(jsonPart)
                    if (jsonEndIndex > 0) {
                        jsonPart.substring(0, jsonEndIndex)
                    } else {
                        jsonPart
                    }
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

    /**
     * Find the end index of a complete JSON object in the given string
     * This helps to exclude trailing characters like newlines
     */
    private fun findJsonEndIndex(jsonPart: String): Int {
        var braceCount = 0
        var inString = false
        var escapeNext = false

        for (i in jsonPart.indices) {
            val char = jsonPart[i]

            if (escapeNext) {
                escapeNext = false
                continue
            }

            if (char == '\\') {
                escapeNext = true
                continue
            }

            if (char == '"' && !escapeNext) {
                inString = !inString
                continue
            }

            if (!inString) {
                when (char) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0) {
                            return i + 1
                        }
                    }
                }
            }
        }

        return -1 // No complete JSON found
    }

    override fun clear() {
        buffer.clear()
    }

    /**
     * Process a complete chunk to extract the actual data
     * Handles SSE format: "data: {json}\n\n"
     * According to SSE specification, multiple data: lines should be concatenated with newlines
     */
    private fun processChunk(chunk: String): String? {
        val data = chunk.lines()
            .filter { it.startsWith("data: ") }
            .joinToString(separator = "\n") { it.substring(6) }

        if (data.trim() == "[DONE]" || data.isBlank()) {
            return null
        }
        return data
    }
}
