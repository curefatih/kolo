package com.fatihcure.kolo.normalizers.anthropic

/**
 * Anthropic delta format
 */
data class AnthropicDelta(
    val type: String,
    val text: String? = null,
)
