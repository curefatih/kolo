package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StreamingKoloServiceTest {

    @Test
    fun `test streaming service creation`() {
        val service = DefaultStreamingKoloService()
        assertNotNull(service)
    }

    @Test
    fun `test global streaming service`() {
        val service = GlobalStreamingKoloService
        assertNotNull(service)
    }

    @Test
    fun `test supports streaming data processing with unknown type`() {
        val service = DefaultStreamingKoloService()
        val isSupported = service.supportsStreamingDataProcessing(UnknownStreamEvent::class)
        assertFalse(isSupported)
    }

    // Test class for unknown stream event type
    data class UnknownStreamEvent(val data: String)
}
