package com.fatihcure.kolo.normalizers.openai

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI streaming response format - matches the actual streaming API response
 */
data class OpenAIStreamingResponse @JsonCreator constructor(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("object") val objectType: String? = null,
    @JsonProperty("created") val created: Long? = null,
    @JsonProperty("model") val model: String? = null,
    @JsonProperty("system_fingerprint") val systemFingerprint: String? = null,
    @JsonProperty("choices") val choices: List<OpenAIStreamingChoice>? = null,
    @JsonProperty("usage") val usage: OpenAIUsage? = null,
    @JsonProperty("error") val error: OpenAIError? = null,
)

/**
 * Choice in streaming response
 */
data class OpenAIStreamingChoice @JsonCreator constructor(
    @JsonProperty("index") val index: Int,
    @JsonProperty("delta") val delta: OpenAIStreamingDelta? = null,
    @JsonProperty("logprobs") val logProbs: Any? = null,
    @JsonProperty("finish_reason") val finishReason: String? = null,
)

/**
 * Delta in streaming response
 */
data class OpenAIStreamingDelta @JsonCreator constructor(
    @JsonProperty("role") val role: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("refusal") val refusal: String? = null,
)
