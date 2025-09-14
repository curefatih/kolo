package com.fatihcure.kolo.core

/**
 * Exception thrown when the data buffer size exceeds the maximum allowed size
 * @param currentSize Current buffer size
 * @param maxSize Maximum allowed buffer size
 * @param additionalDataSize Size of data that would be added
 */
class BufferSizeExceededException(
    val currentSize: Int,
    val maxSize: Int,
    val additionalDataSize: Int,
) : RuntimeException(
    "Buffer size exceeded: current size $currentSize + additional data $additionalDataSize > max size $maxSize",
)
