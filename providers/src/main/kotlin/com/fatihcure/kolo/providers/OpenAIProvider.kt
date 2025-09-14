package com.fatihcure.kolo.providers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fatihcure.kolo.core.AutoRegisterProvider
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.IntermittentUsage
import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.core.Provider
import com.fatihcure.kolo.normalizers.openai.OpenAIChoice
import com.fatihcure.kolo.normalizers.openai.OpenAIDelta
import com.fatihcure.kolo.normalizers.openai.OpenAIError
import com.fatihcure.kolo.normalizers.openai.OpenAINormalizer
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamingResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIUsage
import com.fatihcure.kolo.normalizers.openai.createOpenAIStreamingHandler
import com.fatihcure.kolo.transformers.openai.OpenAITransformer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * OpenAI provider implementation that combines normalizer and transformer
 */
@AutoRegisterProvider(OpenAIRequest::class, OpenAIResponse::class)
class OpenAIProvider(
    private val config: OpenAIProviderConfig = OpenAIProviderConfig.default(),
) : Provider<OpenAIRequest, OpenAIResponse> {

    private val normalizer = OpenAINormalizer()
    private val transformer = OpenAITransformer()
    private val streamingHandler = createOpenAIStreamingHandler(config.objectMapper, config.dataBufferFactory)

    /**
     * Constructor for backward compatibility
     */
    constructor(
        objectMapper: ObjectMapper = ObjectMapper(),
    ) : this(
        config = OpenAIProviderConfig.withObjectMapper(objectMapper),
    )

    // Request normalization and transformation
    override fun normalizeRequest(request: OpenAIRequest): IntermittentRequest {
        return normalizer.normalizeRequest(request)
    }

    override fun transformRequest(request: IntermittentRequest): OpenAIRequest {
        return transformer.transformRequest(request)
    }

    // Response normalization and transformation
    override fun normalizeResponse(response: OpenAIResponse): IntermittentResponse {
        return normalizer.normalizeResponse(response)
    }

    override fun transformResponse(response: IntermittentResponse): OpenAIResponse {
        return transformer.transformResponse(response)
    }

    // Streaming support
    // TODO: will be removed
    override fun normalizeStreamingResponse(stream: Flow<OpenAIResponse>): Flow<IntermittentStreamEvent> {
        // Convert OpenAIResponse to a single IntermittentStreamEvent
        // This is for non-streaming responses converted to streaming format
        // For actual streaming, use processStreamingData() method instead
        return stream.map { response ->
            IntermittentStreamEvent.MessageEnd(
                finishReason = response.choices.firstOrNull()?.finishReason ?: "stop",
                usage = response.usage?.let { usage ->
                    IntermittentUsage(
                        promptTokens = usage.promptTokens,
                        completionTokens = usage.completionTokens,
                        totalTokens = usage.totalTokens,
                    )
                },
            )
        }
    }

    override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<OpenAIResponse> {
        return stream.map { event ->
            val streamEvent = when (event) {
                is IntermittentStreamEvent.MessageStart -> OpenAIStreamEvent(
                    id = event.id,
                    model = event.model,
                )
                is IntermittentStreamEvent.MessageDelta -> OpenAIStreamEvent(
                    choices = listOf(
                        OpenAIChoice(
                            index = 0,
                            delta = OpenAIDelta(
                                role = when (event.delta.role) {
                                    MessageRole.SYSTEM -> "system"
                                    MessageRole.USER -> "user"
                                    MessageRole.ASSISTANT -> "assistant"
                                    MessageRole.TOOL -> "tool"
                                    null -> null
                                },
                                content = event.delta.content,
                                name = event.delta.name,
                            ),
                        ),
                    ),
                )
                is IntermittentStreamEvent.MessageEnd -> OpenAIStreamEvent(
                    choices = listOf(
                        OpenAIChoice(
                            index = 0,
                            finishReason = event.finishReason,
                        ),
                    ),
                    usage = event.usage?.let { usage ->
                        OpenAIUsage(
                            promptTokens = usage.promptTokens,
                            completionTokens = usage.completionTokens,
                            totalTokens = usage.totalTokens,
                        )
                    },
                )
                is IntermittentStreamEvent.Error -> OpenAIStreamEvent(
                    error = OpenAIError(
                        type = event.error.type,
                        message = event.error.message,
                        code = event.error.code,
                        param = event.error.param,
                    ),
                )
            }

            OpenAIResponse(
                id = streamEvent.id ?: "",
                model = streamEvent.model ?: "",
                choices = streamEvent.choices ?: emptyList(),
                usage = streamEvent.usage,
            )
        }
    }

    /**
     * Process raw streaming data and convert to Flow<IntermittentStreamEvent>
     * This method handles the actual streaming data processing with buffering
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of IntermittentStreamEvent objects
     */
    fun processStreamingData(rawStream: Flow<String>): Flow<IntermittentStreamEvent> {
        val streamingResponses = streamingHandler.processStreamingData(rawStream)
        return normalizer.normalizeStreamingResponse(streamingResponses)
    }

    /**
     * Process raw streaming data and convert to Flow<OpenAIStreamingResponse>
     * This method handles the actual streaming data processing with buffering
     * @param rawStream Flow of raw string data from the streaming response
     * @return Flow of OpenAIStreamingResponse objects
     */
    fun processStreamingDataToStreamingResponse(rawStream: Flow<String>): Flow<OpenAIStreamingResponse> {
        return streamingHandler.processStreamingData(rawStream)
    }

    // Error handling
    override fun normalizeError(error: OpenAIResponse): IntermittentError {
        // Convert OpenAIResponse to OpenAIError for error handling
        return IntermittentError(
            type = "response_error",
            message = "Error in response processing",
        )
    }

    override fun transformError(error: IntermittentError): OpenAIResponse {
        // Convert IntermittentError to OpenAIResponse
        return OpenAIResponse(
            id = "",
            model = "",
            choices = emptyList(),
            usage = null,
        )
    }
}
