package com.fatihcure.kolo.normalizers.openai

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI request format
 */
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double? = null,
    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,
    @JsonProperty("top_p")
    val topP: Double? = null,
    @JsonProperty("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @JsonProperty("presence_penalty")
    val presencePenalty: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
)
