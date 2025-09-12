package com.fatihcure.kolo.core

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * The core intermittent format that serves as a universal representation
 * for LLM requests and responses across different providers.
 */

/**
 * Represents a message in the intermittent format
 */
data class IntermittentMessage(
    val role: MessageRole,
    val content: String,
    val name: String? = null,
    val metadata: Map<String, Any>? = null,
)

/**
 * Enumeration of message roles in the intermittent format
 */
enum class MessageRole {
    @JsonProperty("system")
    SYSTEM,

    @JsonProperty("user")
    USER,

    @JsonProperty("assistant")
    ASSISTANT,

    @JsonProperty("tool")
    TOOL,
}

/**
 * Represents a request in the intermittent format
 */
data class IntermittentRequest(
    val messages: List<IntermittentMessage>,
    val model: String,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
    val metadata: Map<String, Any>? = null,
)

/**
 * Represents a response in the intermittent format
 */
data class IntermittentResponse(
    val id: String,
    val model: String,
    val choices: List<IntermittentChoice>,
    val usage: IntermittentUsage? = null,
    val metadata: Map<String, Any>? = null,
)

/**
 * Represents a choice in the intermittent response
 */
data class IntermittentChoice(
    val index: Int,
    val message: IntermittentMessage? = null,
    val delta: IntermittentDelta? = null,
    val finishReason: String? = null,
)

/**
 * Represents a delta in streaming responses
 */
data class IntermittentDelta(
    val role: MessageRole? = null,
    val content: String? = null,
    val name: String? = null,
)

/**
 * Represents usage statistics
 */
data class IntermittentUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

/**
 * Represents an error in the intermittent format
 */
data class IntermittentError(
    val type: String,
    val message: String,
    val code: String? = null,
    val param: String? = null,
    val metadata: Map<String, Any>? = null,
)

/**
 * Represents a streaming event in the intermittent format
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = IntermittentStreamEvent.MessageStart::class, name = "message_start"),
    JsonSubTypes.Type(value = IntermittentStreamEvent.MessageDelta::class, name = "message_delta"),
    JsonSubTypes.Type(value = IntermittentStreamEvent.MessageEnd::class, name = "message_end"),
    JsonSubTypes.Type(value = IntermittentStreamEvent.Error::class, name = "error"),
)
sealed class IntermittentStreamEvent {
    data class MessageStart(
        val id: String,
        val model: String,
        val metadata: Map<String, Any>? = null,
    ) : IntermittentStreamEvent()

    data class MessageDelta(
        val delta: IntermittentDelta,
        val metadata: Map<String, Any>? = null,
    ) : IntermittentStreamEvent()

    data class MessageEnd(
        val finishReason: String? = null,
        val usage: IntermittentUsage? = null,
        val metadata: Map<String, Any>? = null,
    ) : IntermittentStreamEvent()

    data class Error(
        val error: IntermittentError,
        val metadata: Map<String, Any>? = null,
    ) : IntermittentStreamEvent()
}
