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
