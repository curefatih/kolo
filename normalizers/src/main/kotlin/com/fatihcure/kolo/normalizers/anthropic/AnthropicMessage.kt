package com.fatihcure.kolo.normalizers.anthropic

/**
 * Anthropic message format
 */
data class AnthropicMessage(
    val role: String,
    val content: String,
)
