package com.fatihcure.kolo.normalizers.openai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fatihcure.kolo.core.DataBufferFactory
import com.fatihcure.kolo.core.StreamingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Handler for OpenAI streaming responses that processes raw streaming data
 * and converts it to structured OpenAIStreamEvent objects
 */
class OpenAIStreamingHandler(
    private val objectMapper: ObjectMapper,
    private val config: StreamingConfig = StreamingConfig(),
) {

    /**
     * Process raw streaming data and convert to Flow<OpenAIStreamingResponse>
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of parsed OpenAIStreamingResponse objects
     */
    fun processStreamingData(rawStream: Flow<String>): Flow<OpenAIStreamingResponse> {
        return flow {
            val dataBuffer = config.dataBufferFactory.createBuffer()

            rawStream.collect { rawData ->
                val completeChunks = dataBuffer.addChunk(rawData)

                for (chunk in completeChunks) {
                    try {
                        val streamingResponse = parseChunkToStreamingResponse(chunk)
                        if (streamingResponse != null) {
                            emit(streamingResponse)
                        }
                    } catch (e: Exception) {
                        emit(createErrorStreamingResponse("Failed to parse chunk: ${e.message}"))
                    }
                }
            }

            val remainingData = dataBuffer.getRemainingData()
            if (remainingData != null && remainingData.isNotBlank()) {
                try {
                    val streamingResponse = parseChunkToStreamingResponse(remainingData)
                    if (streamingResponse != null) {
                        emit(streamingResponse)
                    }
                } catch (e: Exception) {
                    // Don't emit error for incomplete data - just ignore it
                    // This handles cases where the stream ends with incomplete JSON
                }
            }
        }
    }

    /**
     * Parse a complete JSON chunk to OpenAIStreamingResponse
     */
    private fun parseChunkToStreamingResponse(jsonChunk: String): OpenAIStreamingResponse? {
        if (jsonChunk.trim().isEmpty()) {
            return null
        }

        return try {
            objectMapper.readValue(jsonChunk, OpenAIStreamingResponse::class.java)
        } catch (e: Exception) {
            createErrorStreamingResponse("JSON parsing failed: ${e.message}")
        }
    }

    /**
     * Create an error streaming response
     */
    private fun createErrorStreamingResponse(message: String): OpenAIStreamingResponse {
        return OpenAIStreamingResponse(
            error = OpenAIError(
                type = "parsing_error",
                message = message,
            ),
        )
    }
}

/**
 * Extension function to create a streaming handler with default configuration
 */
fun createOpenAIStreamingHandler(objectMapper: ObjectMapper): OpenAIStreamingHandler {
    return OpenAIStreamingHandler(objectMapper)
}

/**
 * Extension function to create a streaming handler with custom data buffer factory
 */
fun createOpenAIStreamingHandler(
    objectMapper: ObjectMapper,
    dataBufferFactory: DataBufferFactory,
): OpenAIStreamingHandler {
    return OpenAIStreamingHandler(objectMapper, StreamingConfig(dataBufferFactory = dataBufferFactory))
}
