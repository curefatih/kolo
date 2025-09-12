package com.fatihcure.kolo.normalizers.anthropic

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Anthropic request format
 */
data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val system: String? = null,
    val temperature: Double? = null,
    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,
    @JsonProperty("top_p")
    val topP: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
)
