package com.fatihcure.kolo.normalizers.openai

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI choice format
 */
data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage? = null,
    val delta: OpenAIDelta? = null,
    @JsonProperty("finish_reason")
    val finishReason: String? = null,
)
