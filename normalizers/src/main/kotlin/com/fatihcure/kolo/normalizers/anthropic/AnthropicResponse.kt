package com.fatihcure.kolo.normalizers.anthropic

/**
 * Anthropic response format
 */
data class AnthropicResponse(
    val id: String,
    val model: String,
    val content: List<AnthropicContent>,
    val usage: AnthropicUsage? = null,
)
