package com.fatihcure.kolo.core

/**
 * Interface for buffering streaming data to handle partial chunks
 */
interface DataBuffer {
    /**
     * Add data chunk to the buffer
     * @param data The partial data chunk
     * @return List of complete data chunks that can be processed
     */
    fun addChunk(data: String): List<String>

    /**
     * Get any remaining buffered data
     * @return Remaining data in the buffer
     */
    fun getRemainingData(): String?

    /**
     * Clear the buffer
     */
    fun clear()
}

/**
 * Abstract base class for DataBuffer implementations that provides common functionality
 * and requires StreamingConfig for configuration
 */
abstract class AbstractDataBuffer(
    protected val config: StreamingConfig,
) : DataBuffer {

    protected val buffer = StringBuilder()

    /**
     * Check if the buffer size exceeds the maximum allowed size
     * @return true if buffer size is within limits, false if it exceeds maxBufferSize
     */
    protected fun isBufferSizeValid(): Boolean {
        return buffer.length <= config.maxBufferSize
    }

    /**
     * Get the configured chunk separator
     */
    protected fun getChunkSeparator(): String {
        return config.chunkSeparator
    }
}
