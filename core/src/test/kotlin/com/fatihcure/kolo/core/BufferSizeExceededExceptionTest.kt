package com.fatihcure.kolo.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BufferSizeExceededExceptionTest {

    @Test
    fun `should create exception with correct message and properties`() {
        val currentSize = 100
        val maxSize = 200
        val additionalDataSize = 150

        val exception = BufferSizeExceededException(
            currentSize = currentSize,
            maxSize = maxSize,
            additionalDataSize = additionalDataSize,
        )

        assertThat(exception.currentSize).isEqualTo(currentSize)
        assertThat(exception.maxSize).isEqualTo(maxSize)
        assertThat(exception.additionalDataSize).isEqualTo(additionalDataSize)
        assertThat(exception.message).isEqualTo(
            "Buffer size exceeded: current size $currentSize + additional data $additionalDataSize > max size $maxSize",
        )
    }

    @Test
    fun `should be instance of RuntimeException`() {
        val exception = BufferSizeExceededException(
            currentSize = 10,
            maxSize = 20,
            additionalDataSize = 15,
        )

        assertThat(exception).isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `should handle zero values correctly`() {
        val exception = BufferSizeExceededException(
            currentSize = 0,
            maxSize = 0,
            additionalDataSize = 1,
        )

        assertThat(exception.currentSize).isEqualTo(0)
        assertThat(exception.maxSize).isEqualTo(0)
        assertThat(exception.additionalDataSize).isEqualTo(1)
        assertThat(exception.message).isEqualTo(
            "Buffer size exceeded: current size 0 + additional data 1 > max size 0",
        )
    }

    @Test
    fun `should handle large values correctly`() {
        val currentSize = 1000000
        val maxSize = 2000000
        val additionalDataSize = 1500000

        val exception = BufferSizeExceededException(
            currentSize = currentSize,
            maxSize = maxSize,
            additionalDataSize = additionalDataSize,
        )

        assertThat(exception.currentSize).isEqualTo(currentSize)
        assertThat(exception.maxSize).isEqualTo(maxSize)
        assertThat(exception.additionalDataSize).isEqualTo(additionalDataSize)
        assertThat(exception.message).contains("current size $currentSize")
        assertThat(exception.message).contains("additional data $additionalDataSize")
        assertThat(exception.message).contains("max size $maxSize")
    }
}
