package com.fatihcure.kolo.normalizers.anthropic

/**
 * Anthropic error format
 */
data class AnthropicError(
    val type: String,
    val message: String,
)
