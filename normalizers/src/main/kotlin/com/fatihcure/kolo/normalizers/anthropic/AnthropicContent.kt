package com.fatihcure.kolo.normalizers.anthropic

/**
 * Anthropic content format
 */
data class AnthropicContent(
    val type: String,
    val text: String? = null,
)
