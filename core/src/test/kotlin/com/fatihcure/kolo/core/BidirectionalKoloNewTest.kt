package com.fatihcure.kolo.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test class demonstrating the new BidirectionalKolo design
 * This shows how to convert between OpenAI and Anthropic formats
 */
class BidirectionalKoloNewTest {

    // Mock data types for testing
    data class MockOpenAIRequest(val messages: List<MockMessage>, val model: String)
    data class MockOpenAIResponse(val id: String, val model: String, val choices: List<MockChoice>)
    data class MockAnthropicRequest(val messages: List<MockMessage>, val model: String)
    data class MockAnthropicResponse(val id: String, val model: String, val content: List<MockContent>)

    data class MockMessage(val role: String, val content: String)
    data class MockChoice(val index: Int, val message: MockMessage)
    data class MockContent(val type: String, val text: String)

    @Test
    fun `should convert OpenAI request to Anthropic request`() = runBlocking {
        // Create mock normalizers and transformers
        val openAINormalizer = object : Normalizer<MockOpenAIRequest> {
            override fun normalizeRequest(request: MockOpenAIRequest): IntermittentRequest {
                return IntermittentRequest(
                    messages = request.messages.map {
                        IntermittentMessage(
                            role = when (it.role) {
                                "user" -> MessageRole.USER
                                "assistant" -> MessageRole.ASSISTANT
                                else -> MessageRole.USER
                            },
                            content = it.content,
                        )
                    },
                    model = request.model,
                )
            }

            override fun normalizeResponse(response: MockOpenAIRequest): IntermittentResponse {
                throw UnsupportedOperationException("Request type cannot be used as response")
            }

            override fun normalizeStreamingResponse(stream: Flow<MockOpenAIRequest>): Flow<IntermittentStreamEvent> {
                throw UnsupportedOperationException("Request type cannot be used for streaming")
            }

            override fun normalizeError(error: MockOpenAIRequest): IntermittentError {
                throw UnsupportedOperationException("Request type cannot be used for errors")
            }
        }

        val anthropicTransformer = object : Transformer<MockAnthropicRequest, MockAnthropicResponse, MockAnthropicResponse> {
            override fun transformRequest(request: IntermittentRequest): MockAnthropicRequest {
                return MockAnthropicRequest(
                    messages = request.messages.map {
                        MockMessage(
                            role = when (it.role) {
                                MessageRole.USER -> "user"
                                MessageRole.ASSISTANT -> "assistant"
                                MessageRole.SYSTEM -> "user" // Anthropic doesn't have system role
                                else -> "user"
                            },
                            content = it.content,
                        )
                    },
                    model = request.model,
                )
            }

            override fun transformResponse(response: IntermittentResponse): MockAnthropicResponse {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }

            override fun transformError(error: IntermittentError): MockAnthropicResponse {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }
        }

        val anthropicNormalizer = object : Normalizer<MockAnthropicResponse> {
            override fun normalizeRequest(request: MockAnthropicResponse): IntermittentRequest {
                throw UnsupportedOperationException()
            }
            override fun normalizeResponse(response: MockAnthropicResponse): IntermittentResponse {
                return IntermittentResponse(
                    id = response.id,
                    model = response.model,
                    choices = listOf(
                        IntermittentChoice(
                            index = 0,
                            message = IntermittentMessage(
                                role = MessageRole.ASSISTANT,
                                content = response.content.joinToString("") { it.text },
                            ),
                        ),
                    ),
                )
            }

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<MockAnthropicResponse>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                throw UnsupportedOperationException()
            }

            override fun normalizeError(error: MockAnthropicResponse): IntermittentError {
                throw UnsupportedOperationException()
            }
        }

        val openAITransformer = object : Transformer<MockOpenAIRequest, MockOpenAIResponse, MockOpenAIResponse> {
            override fun transformRequest(request: IntermittentRequest): MockOpenAIRequest {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }

            override fun transformResponse(response: IntermittentResponse): MockOpenAIResponse {
                return MockOpenAIResponse(
                    id = response.id,
                    model = response.model,
                    choices = response.choices.map {
                        MockChoice(
                            index = it.index,
                            message = MockMessage(
                                role = when (it.message?.role) {
                                    MessageRole.USER -> "user"
                                    MessageRole.ASSISTANT -> "assistant"
                                    MessageRole.SYSTEM -> "system"
                                    else -> "user"
                                },
                                content = it.message?.content ?: "",
                            ),
                        )
                    },
                )
            }

            override fun transformError(error: IntermittentError): MockOpenAIResponse {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }
        }

        // Create BidirectionalKolo instance
        val bidirectionalKolo = BidirectionalKolo(
            sourceNormalizer = openAINormalizer,
            sourceTransformer = openAITransformer,
            targetNormalizer = anthropicNormalizer,
            targetTransformer = anthropicTransformer,
        )

        // Test request conversion: OpenAI → Anthropic
        val openAIRequest = MockOpenAIRequest(
            messages = listOf(
                MockMessage(role = "user", content = "Hello, how are you?"),
            ),
            model = "gpt-4",
        )

        val anthropicRequest = bidirectionalKolo.convertRequest(openAIRequest)

        assertThat(anthropicRequest.model).isEqualTo("gpt-4")
        assertThat(anthropicRequest.messages).hasSize(1)
        assertThat(anthropicRequest.messages[0].role).isEqualTo("user")
        assertThat(anthropicRequest.messages[0].content).isEqualTo("Hello, how are you?")
    }

    @Test
    fun `should convert Anthropic response to OpenAI response`() = runBlocking {
        // Create mock normalizers and transformers (same as above)
        val openAINormalizer = object : Normalizer<MockOpenAIRequest> {
            override fun normalizeRequest(request: MockOpenAIRequest): IntermittentRequest {
                return IntermittentRequest(
                    messages = request.messages.map {
                        IntermittentMessage(
                            role = when (it.role) {
                                "user" -> MessageRole.USER
                                "assistant" -> MessageRole.ASSISTANT
                                else -> MessageRole.USER
                            },
                            content = it.content,
                        )
                    },
                    model = request.model,
                )
            }

            override fun normalizeResponse(response: MockOpenAIRequest): IntermittentResponse {
                throw UnsupportedOperationException("Request type cannot be used as response")
            }

            override fun normalizeStreamingResponse(stream: Flow<MockOpenAIRequest>): Flow<IntermittentStreamEvent> {
                throw UnsupportedOperationException("Request type cannot be used for streaming")
            }

            override fun normalizeError(error: MockOpenAIRequest): IntermittentError {
                throw UnsupportedOperationException("Request type cannot be used for errors")
            }
        }

        val anthropicTransformer = object : Transformer<MockAnthropicRequest, MockAnthropicResponse, MockAnthropicResponse> {
            override fun transformRequest(request: IntermittentRequest): MockAnthropicRequest {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }

            override fun transformResponse(response: IntermittentResponse): MockAnthropicResponse {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }

            override fun transformError(error: IntermittentError): MockAnthropicResponse {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }
        }

        val anthropicNormalizer = object : Normalizer<MockAnthropicResponse> {
            override fun normalizeRequest(request: MockAnthropicResponse): IntermittentRequest {
                throw UnsupportedOperationException()
            }
            override fun normalizeResponse(response: MockAnthropicResponse): IntermittentResponse {
                return IntermittentResponse(
                    id = response.id,
                    model = response.model,
                    choices = listOf(
                        IntermittentChoice(
                            index = 0,
                            message = IntermittentMessage(
                                role = MessageRole.ASSISTANT,
                                content = response.content.joinToString("") { it.text },
                            ),
                        ),
                    ),
                )
            }

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<MockAnthropicResponse>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                throw UnsupportedOperationException()
            }

            override fun normalizeError(error: MockAnthropicResponse): IntermittentError {
                throw UnsupportedOperationException()
            }
        }

        val openAITransformer = object : Transformer<MockOpenAIRequest, MockOpenAIResponse, MockOpenAIResponse> {
            override fun transformRequest(request: IntermittentRequest): MockOpenAIRequest {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }

            override fun transformResponse(response: IntermittentResponse): MockOpenAIResponse {
                return MockOpenAIResponse(
                    id = response.id,
                    model = response.model,
                    choices = response.choices.map {
                        MockChoice(
                            index = it.index,
                            message = MockMessage(
                                role = when (it.message?.role) {
                                    MessageRole.USER -> "user"
                                    MessageRole.ASSISTANT -> "assistant"
                                    MessageRole.SYSTEM -> "system"
                                    else -> "user"
                                },
                                content = it.message?.content ?: "",
                            ),
                        )
                    },
                )
            }

            override fun transformError(error: IntermittentError): MockOpenAIResponse {
                // This won't be used in this test
                throw UnsupportedOperationException()
            }
        }

        // Create BidirectionalKolo instance
        val bidirectionalKolo = BidirectionalKolo(
            sourceNormalizer = openAINormalizer,
            sourceTransformer = openAITransformer,
            targetNormalizer = anthropicNormalizer,
            targetTransformer = anthropicTransformer,
        )

        // Test response conversion: Anthropic → OpenAI
        val anthropicResponse = MockAnthropicResponse(
            id = "anthropic-123",
            model = "claude-3",
            content = listOf(
                MockContent(type = "text", text = "I'm doing well, thank you for asking!"),
            ),
        )

        val openAIResponse = bidirectionalKolo.convertResponse(anthropicResponse)

        assertThat(openAIResponse.id).isEqualTo("anthropic-123")
        assertThat(openAIResponse.model).isEqualTo("claude-3")
        assertThat(openAIResponse.choices).hasSize(1)
        assertThat(openAIResponse.choices[0].message.role).isEqualTo("assistant")
        assertThat(openAIResponse.choices[0].message.content).isEqualTo("I'm doing well, thank you for asking!")
    }

    @Test
    fun `should work with builder pattern`() = runBlocking {
        // Create mock normalizers and transformers
        val openAINormalizer = object : Normalizer<MockOpenAIRequest> {
            override fun normalizeRequest(request: MockOpenAIRequest): IntermittentRequest {
                return IntermittentRequest(
                    messages = request.messages.map {
                        IntermittentMessage(
                            role = when (it.role) {
                                "user" -> MessageRole.USER
                                "assistant" -> MessageRole.ASSISTANT
                                else -> MessageRole.USER
                            },
                            content = it.content,
                        )
                    },
                    model = request.model,
                )
            }

            override fun normalizeResponse(response: MockOpenAIRequest): IntermittentResponse {
                throw UnsupportedOperationException("Request type cannot be used as response")
            }

            override fun normalizeStreamingResponse(stream: Flow<MockOpenAIRequest>): Flow<IntermittentStreamEvent> {
                throw UnsupportedOperationException("Request type cannot be used for streaming")
            }

            override fun normalizeError(error: MockOpenAIRequest): IntermittentError {
                throw UnsupportedOperationException("Request type cannot be used for errors")
            }
        }

        val anthropicTransformer = object : Transformer<MockAnthropicRequest, MockAnthropicResponse, MockAnthropicResponse> {
            override fun transformRequest(request: IntermittentRequest): MockAnthropicRequest {
                return MockAnthropicRequest(
                    messages = request.messages.map {
                        MockMessage(
                            role = when (it.role) {
                                MessageRole.USER -> "user"
                                MessageRole.ASSISTANT -> "assistant"
                                MessageRole.SYSTEM -> "user"
                                else -> "user"
                            },
                            content = it.content,
                        )
                    },
                    model = request.model,
                )
            }

            override fun transformResponse(response: IntermittentResponse): MockAnthropicResponse {
                throw UnsupportedOperationException()
            }

            override fun transformError(error: IntermittentError): MockAnthropicResponse {
                throw UnsupportedOperationException()
            }
        }

        val anthropicNormalizer = object : Normalizer<MockAnthropicResponse> {
            override fun normalizeRequest(request: MockAnthropicResponse): IntermittentRequest {
                throw UnsupportedOperationException()
            }
            override fun normalizeResponse(response: MockAnthropicResponse): IntermittentResponse {
                return IntermittentResponse(
                    id = response.id,
                    model = response.model,
                    choices = listOf(
                        IntermittentChoice(
                            index = 0,
                            message = IntermittentMessage(
                                role = MessageRole.ASSISTANT,
                                content = response.content.joinToString("") { it.text },
                            ),
                        ),
                    ),
                )
            }

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<MockAnthropicResponse>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                throw UnsupportedOperationException()
            }

            override fun normalizeError(error: MockAnthropicResponse): IntermittentError {
                throw UnsupportedOperationException()
            }
        }

        val openAITransformer = object : Transformer<MockOpenAIRequest, MockOpenAIResponse, MockOpenAIResponse> {
            override fun transformRequest(request: IntermittentRequest): MockOpenAIRequest {
                throw UnsupportedOperationException()
            }

            override fun transformResponse(response: IntermittentResponse): MockOpenAIResponse {
                return MockOpenAIResponse(
                    id = response.id,
                    model = response.model,
                    choices = response.choices.map {
                        MockChoice(
                            index = it.index,
                            message = MockMessage(
                                role = when (it.message?.role) {
                                    MessageRole.USER -> "user"
                                    MessageRole.ASSISTANT -> "assistant"
                                    MessageRole.SYSTEM -> "system"
                                    else -> "user"
                                },
                                content = it.message?.content ?: "",
                            ),
                        )
                    },
                )
            }

            override fun transformError(error: IntermittentError): MockOpenAIResponse {
                throw UnsupportedOperationException()
            }
        }

        // Use builder pattern
        val bidirectionalKolo = bidirectionalKoloBuilder<MockOpenAIRequest, MockOpenAIResponse, MockAnthropicRequest, MockAnthropicResponse>()
            .withSourceNormalizer(openAINormalizer)
            .withSourceTransformer(openAITransformer)
            .withTargetNormalizer(anthropicNormalizer)
            .withTargetTransformer(anthropicTransformer)
            .build()

        // Test request conversion
        val openAIRequest = MockOpenAIRequest(
            messages = listOf(
                MockMessage(role = "user", content = "Hello!"),
            ),
            model = "gpt-4",
        )

        val anthropicRequest = bidirectionalKolo.convertRequest(openAIRequest)

        assertThat(anthropicRequest.model).isEqualTo("gpt-4")
        assertThat(anthropicRequest.messages).hasSize(1)
        assertThat(anthropicRequest.messages[0].content).isEqualTo("Hello!")
    }
}
