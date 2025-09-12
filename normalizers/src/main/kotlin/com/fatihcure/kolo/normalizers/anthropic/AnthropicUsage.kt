package com.fatihcure.kolo.normalizers.anthropic

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Anthropic usage format
 */
data class AnthropicUsage(
    @JsonProperty("input_tokens")
    val inputTokens: Int,
    @JsonProperty("output_tokens")
    val outputTokens: Int,
)
