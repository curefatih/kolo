package com.fatihcure.kolo.normalizers.openai

/**
 * OpenAI error format
 */
data class OpenAIError(
    val type: String,
    val message: String,
    val code: String? = null,
    val param: String? = null,
)
