package com.fatihcure.kolo.normalizers.anthropic

/**
 * Anthropic streaming event format
 */
data class AnthropicStreamEvent(
    val type: String,
    val id: String? = null,
    val model: String? = null,
    val content: List<AnthropicContent>? = null,
    val usage: AnthropicUsage? = null,
    val error: AnthropicError? = null,
    val delta: AnthropicDelta? = null,
)
