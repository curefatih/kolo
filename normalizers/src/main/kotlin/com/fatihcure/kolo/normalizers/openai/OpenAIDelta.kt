package com.fatihcure.kolo.normalizers.openai

/**
 * OpenAI delta format
 */
data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null,
    val name: String? = null,
)
