package com.fatihcure.kolo.normalizers.openai

import com.fatihcure.kolo.core.AutoRegisterNormalizer
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
 * Normalizer implementation for OpenAI API
 */
@AutoRegisterNormalizer(OpenAIRequest::class)
@AutoRegisterNormalizer(OpenAIResponse::class)
@AutoRegisterNormalizer(OpenAIStreamingResponse::class)
@AutoRegisterNormalizer(OpenAIError::class)
class OpenAINormalizer : RequestNormalizer<OpenAIRequest>, ResponseNormalizer<OpenAIResponse>, StreamingNormalizer<OpenAIStreamingResponse>, ErrorNormalizer<OpenAIError> {

    override fun normalizeRequest(request: OpenAIRequest): IntermittentRequest {
        return IntermittentRequest(
            messages = request.messages.map { normalizeMessage(it) },
            model = request.model,
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            topP = request.topP,
            frequencyPenalty = request.frequencyPenalty,
            presencePenalty = request.presencePenalty,
            stop = request.stop,
            stream = request.stream,
        )
    }

    override fun normalizeResponse(response: OpenAIResponse): IntermittentResponse {
        return IntermittentResponse(
            id = response.id,
            model = response.model,
            choices = response.choices.map { normalizeChoice(it) },
            usage = response.usage?.let { normalizeUsage(it) },
        )
    }

    override fun normalizeStreamingResponse(stream: Flow<OpenAIStreamingResponse>): Flow<IntermittentStreamEvent> {
        return stream.map { response ->
            when {
                response.error != null -> IntermittentStreamEvent.Error(
                    error = normalizeError(response.error!!),
                )
                response.choices?.isNotEmpty() == true -> {
                    val choice = response.choices.first()
                    when {
                        choice.delta != null && choice.delta.role != null -> IntermittentStreamEvent.MessageStart(
                            id = response.id ?: "",
                            model = response.model ?: "",
                        )
                        choice.delta != null && choice.delta.content != null -> IntermittentStreamEvent.MessageDelta(
                            delta = normalizeStreamingDelta(choice.delta!!),
                        )
                        choice.finishReason != null -> IntermittentStreamEvent.MessageEnd(
                            finishReason = choice.finishReason,
                            usage = response.usage?.let { normalizeUsage(it) },
                        )
                        else -> throw IllegalArgumentException("Unknown OpenAI streaming choice type in response: $response")
                    }
                }
                response.choices.isNullOrEmpty() && response.usage != null -> {
                    IntermittentStreamEvent.MessageEnd(
                        finishReason = "stop", // Using 'stop' as a default for usage-only chunks
                        usage = normalizeUsage(response.usage!!),
                    )
                }
                else -> throw IllegalArgumentException("Unknown OpenAI streaming response type: $response")
            }
        }
    }

    override fun normalizeError(error: OpenAIError): IntermittentError {
        return IntermittentError(
            type = error.type,
            message = error.message,
            code = error.code,
            param = error.param,
        )
    }

    internal fun normalizeMessage(message: OpenAIMessage): IntermittentMessage {
        return IntermittentMessage(
            role = when (message.role.lowercase()) {
                "system" -> MessageRole.SYSTEM
                "user" -> MessageRole.USER
                "assistant" -> MessageRole.ASSISTANT
                "tool" -> MessageRole.TOOL
                else -> throw IllegalArgumentException("Unknown OpenAI message role: ${message.role}")
            },
            content = message.content,
            name = message.name,
        )
    }

    internal fun normalizeChoice(choice: OpenAIChoice): IntermittentChoice {
        return IntermittentChoice(
            index = choice.index,
            message = choice.message?.let { normalizeMessage(it) },
            delta = choice.delta?.let { normalizeDelta(it) },
            finishReason = choice.finishReason,
        )
    }

    internal fun normalizeDelta(delta: OpenAIDelta): IntermittentDelta {
        return IntermittentDelta(
            role = delta.role?.let {
                when (it.lowercase()) {
                    "system" -> MessageRole.SYSTEM
                    "user" -> MessageRole.USER
                    "assistant" -> MessageRole.ASSISTANT
                    "tool" -> MessageRole.TOOL
                    else -> throw IllegalArgumentException("Unknown OpenAI delta role: $it")
                }
            },
            content = delta.content,
            name = delta.name,
        )
    }

    internal fun normalizeUsage(usage: OpenAIUsage): IntermittentUsage {
        return IntermittentUsage(
            promptTokens = usage.promptTokens,
            completionTokens = usage.completionTokens,
            totalTokens = usage.totalTokens,
        )
    }

    internal fun normalizeStreamingDelta(delta: OpenAIStreamingDelta): IntermittentDelta {
        return IntermittentDelta(
            role = delta.role?.let {
                when (it.lowercase()) {
                    "system" -> MessageRole.SYSTEM
                    "user" -> MessageRole.USER
                    "assistant" -> MessageRole.ASSISTANT
                    "tool" -> MessageRole.TOOL
                    else -> throw IllegalArgumentException("Unknown OpenAI delta role: $it")
                }
            },
            content = delta.content,
            name = delta.name,
        )
    }
}
