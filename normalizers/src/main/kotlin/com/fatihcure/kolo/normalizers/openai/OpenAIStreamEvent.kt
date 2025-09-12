package com.fatihcure.kolo.normalizers.openai

/**
 * OpenAI streaming event format
 */
data class OpenAIStreamEvent(
    val id: String? = null,
    val model: String? = null,
    val choices: List<OpenAIChoice>? = null,
    val usage: OpenAIUsage? = null,
    val error: OpenAIError? = null,
)
