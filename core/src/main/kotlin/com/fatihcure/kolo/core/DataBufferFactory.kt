package com.fatihcure.kolo.core

/**
 * Factory interface for creating DataBuffer instances.
 * This allows creating different buffers for different requests.
 */
interface DataBufferFactory {
    /**
     * Creates a new DataBuffer instance.
     * @param config The streaming configuration to use for the buffer
     * @return A new DataBuffer instance
     */
    fun createBuffer(config: StreamingConfig): DataBuffer
}

/**
 * Default implementation of DataBufferFactory that creates DefaultDataBuffer instances.
 */
class DefaultDataBufferFactory : DataBufferFactory {
    override fun createBuffer(config: StreamingConfig): DataBuffer {
        return DefaultDataBuffer(config)
    }
}
