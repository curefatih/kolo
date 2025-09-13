package com.fatihcure.kolo.core

/**
 * Configuration for streaming data handling
 */
data class StreamingConfig(
    val dataBufferFactory: DataBufferFactory = DefaultDataBufferFactory(),
    val maxBufferSize: Int = 1024 * 1024,
    val chunkSeparator: String = "\n\n",
)
