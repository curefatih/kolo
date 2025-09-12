package com.fatihcure.kolo.normalizers.openai

/**
 * OpenAI response format
 */
data class OpenAIResponse(
    val id: String,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null,
)
