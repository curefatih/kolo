package com.fatihcure.kolo.normalizers.openai

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI usage format
 */
data class OpenAIUsage @JsonCreator constructor(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int,
    @JsonProperty("completion_tokens")
    val completionTokens: Int,
    @JsonProperty("total_tokens")
    val totalTokens: Int,
    @JsonProperty("prompt_tokens_details")
    val promptTokensDetails: PromptTokensDetails? = null,
    @JsonProperty("completion_tokens_details")
    val completionTokensDetails: CompletionTokensDetails? = null,
)

/**
 * Prompt tokens details
 */
data class PromptTokensDetails @JsonCreator constructor(
    @JsonProperty("cached_tokens")
    val cachedTokens: Int? = null,
    @JsonProperty("audio_tokens")
    val audioTokens: Int? = null,
)

/**
 * Completion tokens details
 */
data class CompletionTokensDetails @JsonCreator constructor(
    @JsonProperty("reasoning_tokens")
    val reasoningTokens: Int? = null,
    @JsonProperty("audio_tokens")
    val audioTokens: Int? = null,
    @JsonProperty("accepted_prediction_tokens")
    val acceptedPredictionTokens: Int? = null,
    @JsonProperty("rejected_prediction_tokens")
    val rejectedPredictionTokens: Int? = null,
)
