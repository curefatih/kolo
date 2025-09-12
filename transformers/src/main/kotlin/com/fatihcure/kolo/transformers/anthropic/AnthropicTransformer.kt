package com.fatihcure.kolo.transformers.anthropic

import com.fatihcure.kolo.core.ErrorTransformer
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentMessage
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.IntermittentUsage
import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.core.RequestTransformer
import com.fatihcure.kolo.core.ResponseTransformer
import com.fatihcure.kolo.core.StreamingTransformer
import com.fatihcure.kolo.normalizers.anthropic.AnthropicContent
import com.fatihcure.kolo.normalizers.anthropic.AnthropicDelta
import com.fatihcure.kolo.normalizers.anthropic.AnthropicError
import com.fatihcure.kolo.normalizers.anthropic.AnthropicMessage
import com.fatihcure.kolo.normalizers.anthropic.AnthropicRequest
import com.fatihcure.kolo.normalizers.anthropic.AnthropicResponse
import com.fatihcure.kolo.normalizers.anthropic.AnthropicStreamEvent
import com.fatihcure.kolo.normalizers.anthropic.AnthropicUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Transformer implementation for Anthropic Claude API
 */
class AnthropicTransformer : RequestTransformer<AnthropicRequest>, ResponseTransformer<AnthropicResponse>, StreamingTransformer<AnthropicStreamEvent>, ErrorTransformer<AnthropicError> {

    override fun transformRequest(request: IntermittentRequest): AnthropicRequest {
        val systemMessage = request.messages.find { it.role == MessageRole.SYSTEM }
        val otherMessages = request.messages.filter { it.role != MessageRole.SYSTEM }

        return AnthropicRequest(
            model = request.model,
            messages = otherMessages.map { transformMessage(it) },
            system = systemMessage?.content,
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            topP = request.topP,
            stop = request.stop,
            stream = request.stream,
        )
    }

    override fun transformResponse(response: IntermittentResponse): AnthropicResponse {
        val assistantMessage = response.choices
            .find { it.message?.role == MessageRole.ASSISTANT }
            ?.message

        return AnthropicResponse(
            id = response.id,
            model = response.model,
            content = listOf(
                AnthropicContent(
                    type = "text",
                    text = assistantMessage?.content ?: "",
                ),
            ),
            usage = response.usage?.let { transformUsage(it) },
        )
    }

    override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<AnthropicStreamEvent> {
        return stream.map { event ->
            when (event) {
                is IntermittentStreamEvent.MessageStart -> AnthropicStreamEvent(
                    type = "message_start",
                    id = event.id,
                    model = event.model,
                )
                is IntermittentStreamEvent.MessageDelta -> AnthropicStreamEvent(
                    type = "content_block_delta",
                    delta = AnthropicDelta(
                        type = "text_delta",
                        text = event.delta.content,
                    ),
                )
                is IntermittentStreamEvent.MessageEnd -> AnthropicStreamEvent(
                    type = "message_delta",
                    usage = event.usage?.let { transformUsage(it) },
                )
                is IntermittentStreamEvent.Error -> AnthropicStreamEvent(
                    type = "error",
                    error = transformError(event.error),
                )
            }
        }
    }

    override fun transformError(error: IntermittentError): AnthropicError {
        return AnthropicError(
            type = error.type,
            message = error.message,
        )
    }

    private fun transformMessage(message: IntermittentMessage): AnthropicMessage {
        return AnthropicMessage(
            role = when (message.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                else -> throw IllegalArgumentException("Anthropic only supports user and assistant roles in messages")
            },
            content = message.content,
        )
    }

    private fun transformUsage(usage: IntermittentUsage): AnthropicUsage {
        return AnthropicUsage(
            inputTokens = usage.promptTokens,
            outputTokens = usage.completionTokens,
        )
    }
}
