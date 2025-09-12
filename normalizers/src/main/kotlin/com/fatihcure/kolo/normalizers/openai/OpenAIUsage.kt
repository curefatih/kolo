package com.fatihcure.kolo.normalizers.openai

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI usage format
 */
data class OpenAIUsage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int,
    @JsonProperty("completion_tokens")
    val completionTokens: Int,
    @JsonProperty("total_tokens")
    val totalTokens: Int,
)
