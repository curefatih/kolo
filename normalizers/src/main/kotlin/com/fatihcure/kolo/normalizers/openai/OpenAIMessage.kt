package com.fatihcure.kolo.normalizers.openai

/**
 * OpenAI message format
 */
data class OpenAIMessage(
    val role: String,
    val content: String,
    val name: String? = null,
)
