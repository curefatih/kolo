package com.fatihcure.kolo.transformers.openai

import com.fatihcure.kolo.core.AutoRegisterStreamingTransformer
import com.fatihcure.kolo.core.CombinedTransformer
import com.fatihcure.kolo.core.IntermittentChoice
import com.fatihcure.kolo.core.IntermittentDelta
import com.fatihcure.kolo.core.IntermittentError
import com.fatihcure.kolo.core.IntermittentMessage
import com.fatihcure.kolo.core.IntermittentRequest
import com.fatihcure.kolo.core.IntermittentResponse
import com.fatihcure.kolo.core.IntermittentStreamEvent
import com.fatihcure.kolo.core.IntermittentUsage
import com.fatihcure.kolo.core.MessageRole
import com.fatihcure.kolo.normalizers.openai.OpenAIChoice
import com.fatihcure.kolo.normalizers.openai.OpenAIDelta
import com.fatihcure.kolo.normalizers.openai.OpenAIError
import com.fatihcure.kolo.normalizers.openai.OpenAIMessage
import com.fatihcure.kolo.normalizers.openai.OpenAIRequest
import com.fatihcure.kolo.normalizers.openai.OpenAIResponse
import com.fatihcure.kolo.normalizers.openai.OpenAIStreamEvent
import com.fatihcure.kolo.normalizers.openai.OpenAIUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Transformer implementation for OpenAI API
 */
@AutoRegisterStreamingTransformer(OpenAIStreamEvent::class)
class OpenAITransformer : CombinedTransformer<OpenAIRequest, OpenAIResponse, OpenAIError, OpenAIStreamEvent> {

    override fun transformRequest(request: IntermittentRequest): OpenAIRequest {
        return OpenAIRequest(
            model = request.model,
            messages = request.messages.map { transformMessage(it) },
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            topP = request.topP,
            frequencyPenalty = request.frequencyPenalty,
            presencePenalty = request.presencePenalty,
            stop = request.stop,
            stream = request.stream,
        )
    }

    override fun transformResponse(response: IntermittentResponse): OpenAIResponse {
        return OpenAIResponse(
            id = response.id,
            model = response.model,
            choices = response.choices.map { transformChoice(it) },
            usage = response.usage?.let { transformUsage(it) },
        )
    }

    override fun transformStreamingResponse(stream: Flow<IntermittentStreamEvent>): Flow<OpenAIStreamEvent> {
        return stream.map { event ->
            when (event) {
                is IntermittentStreamEvent.MessageStart -> OpenAIStreamEvent(
                    id = event.id,
                    model = event.model,
                )
                is IntermittentStreamEvent.MessageDelta -> OpenAIStreamEvent(
                    choices = listOf(
                        OpenAIChoice(
                            index = 0,
                            delta = transformDelta(event.delta),
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
                    usage = event.usage?.let { transformUsage(it) },
                )
                is IntermittentStreamEvent.Error -> OpenAIStreamEvent(
                    error = transformError(event.error),
                )
            }
        }
    }

    override fun transformError(error: IntermittentError): OpenAIError {
        return OpenAIError(
            type = error.type,
            message = error.message,
            code = error.code,
            param = error.param,
        )
    }

    private fun transformMessage(message: IntermittentMessage): OpenAIMessage {
        return OpenAIMessage(
            role = when (message.role) {
                MessageRole.SYSTEM -> "system"
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.TOOL -> "tool"
            },
            content = message.content,
            name = message.name,
        )
    }

    private fun transformChoice(choice: IntermittentChoice): OpenAIChoice {
        return OpenAIChoice(
            index = choice.index,
            message = choice.message?.let { transformMessage(it) },
            delta = choice.delta?.let { transformDelta(it) },
            finishReason = choice.finishReason,
        )
    }

    private fun transformDelta(delta: IntermittentDelta): OpenAIDelta {
        return OpenAIDelta(
            role = delta.role?.let {
                when (it) {
                    MessageRole.SYSTEM -> "system"
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.TOOL -> "tool"
                }
            },
            content = delta.content,
            name = delta.name,
        )
    }

    private fun transformStreamingDelta(delta: IntermittentDelta): com.fatihcure.kolo.normalizers.openai.OpenAIStreamingDelta {
        return com.fatihcure.kolo.normalizers.openai.OpenAIStreamingDelta(
            role = delta.role?.let {
                when (it) {
                    MessageRole.SYSTEM -> "system"
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.TOOL -> "tool"
                }
            },
            content = delta.content,
            name = delta.name,
        )
    }

    private fun transformUsage(usage: IntermittentUsage): OpenAIUsage {
        return OpenAIUsage(
            promptTokens = usage.promptTokens,
            completionTokens = usage.completionTokens,
            totalTokens = usage.totalTokens,
        )
    }
}
