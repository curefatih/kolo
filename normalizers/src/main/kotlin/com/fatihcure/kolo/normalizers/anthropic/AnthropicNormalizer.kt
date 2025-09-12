package com.fatihcure.kolo.normalizers.anthropic

import com.fatihcure.kolo.core.ErrorNormalizer
import com.fatihcure.kolo.core.IntermittentChoice
import com.fatihcure.kolo.core.IntermittentDelta
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentMessage
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.IntermittentUsage
import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.core.RequestNormalizer
import com.fatihcure.kolo.core.ResponseNormalizer
import com.fatihcure.kolo.core.StreamingNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Normalizer for Anthropic Claude API requests and responses
 */

/**
 * Normalizer implementation for Anthropic Claude API
 */
class AnthropicNormalizer : RequestNormalizer<AnthropicRequest>, ResponseNormalizer<AnthropicResponse>, StreamingNormalizer<AnthropicStreamEvent>, ErrorNormalizer<AnthropicError> {

    override fun normalizeRequest(request: AnthropicRequest): IntermittentRequest {
        val messages = mutableListOf<IntermittentMessage>()

        // Add system message if present
        request.system?.let { system ->
            messages.add(
                IntermittentMessage(
                    role = MessageRole.SYSTEM,
                    content = system,
                ),
            )
        }

        // Add user/assistant messages
        request.messages.forEach { message ->
            messages.add(
                IntermittentMessage(
                    role = when (message.role.lowercase()) {
                        "user" -> MessageRole.USER
                        "assistant" -> MessageRole.ASSISTANT
                        else -> throw IllegalArgumentException("Unknown Anthropic message role: ${message.role}")
                    },
                    content = message.content,
                ),
            )
        }

        return IntermittentRequest(
            messages = messages,
            model = request.model,
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            topP = request.topP,
            stop = request.stop,
            stream = request.stream,
        )
    }

    override fun normalizeResponse(response: AnthropicResponse): IntermittentResponse {
        val textContent = response.content
            .filter { it.type == "text" }
            .joinToString("") { it.text ?: "" }

        return IntermittentResponse(
            id = response.id,
            model = response.model,
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = textContent,
                    ),
                ),
            ),
            usage = response.usage?.let { normalizeUsage(it) },
        )
    }

    override fun normalizeStreamingResponse(stream: Flow<AnthropicStreamEvent>): Flow<IntermittentStreamEvent> {
        return stream.map { event ->
            when (event.type) {
                "message_start" -> IntermittentStreamEvent.MessageStart(
                    id = event.id ?: "",
                    model = event.model ?: "",
                )
                "content_block_delta" -> IntermittentStreamEvent.MessageDelta(
                    delta = IntermittentDelta(
                        content = event.delta?.text,
                    ),
                )
                "message_delta" -> IntermittentStreamEvent.MessageEnd(
                    usage = event.usage?.let { normalizeUsage(it) },
                )
                "error" -> IntermittentStreamEvent.Error(
                    error = normalizeError(event.error!!),
                )
                else -> throw IllegalArgumentException("Unknown Anthropic stream event type: ${event.type}")
            }
        }
    }

    override fun normalizeError(error: AnthropicError): IntermittentError {
        return IntermittentError(
            type = error.type,
            message = error.message,
        )
    }

    internal fun normalizeUsage(usage: AnthropicUsage): IntermittentUsage {
        return IntermittentUsage(
            promptTokens = usage.inputTokens,
            completionTokens = usage.outputTokens,
            totalTokens = usage.inputTokens + usage.outputTokens,
        )
    }
}
