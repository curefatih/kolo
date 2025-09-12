package com.fatihcure.kolo.core

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class IntermittentFormatTest {
    
    @Test
    fun `should create intermittent request with all fields`() {
        val request = IntermittentRequest(
            messages = listOf(
                IntermittentMessage(
                    role = MessageRole.SYSTEM,
                    content = "You are a helpful assistant."
                ),
                IntermittentMessage(
                    role = MessageRole.USER,
                    content = "Hello, how are you?"
                )
            ),
            model = "gpt-3.5-turbo",
            temperature = 0.7,
            maxTokens = 100,
            topP = 0.9,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0,
            stop = listOf("\n", "Human:", "Assistant:"),
            stream = false
        )
        
        assertThat(request.messages).hasSize(2)
        assertThat(request.messages[0].role).isEqualTo(MessageRole.SYSTEM)
        assertThat(request.messages[0].content).isEqualTo("You are a helpful assistant.")
        assertThat(request.messages[1].role).isEqualTo(MessageRole.USER)
        assertThat(request.messages[1].content).isEqualTo("Hello, how are you?")
        assertThat(request.model).isEqualTo("gpt-3.5-turbo")
        assertThat(request.temperature).isEqualTo(0.7)
        assertThat(request.maxTokens).isEqualTo(100)
        assertThat(request.topP).isEqualTo(0.9)
        assertThat(request.frequencyPenalty).isEqualTo(0.0)
        assertThat(request.presencePenalty).isEqualTo(0.0)
        assertThat(request.stop).containsExactly("\n", "Human:", "Assistant:")
        assertThat(request.stream).isFalse()
    }
    
    @Test
    fun `should create intermittent response with all fields`() {
        val response = IntermittentResponse(
            id = "chatcmpl-123",
            model = "gpt-3.5-turbo",
            choices = listOf(
                IntermittentChoice(
                    index = 0,
                    message = IntermittentMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Hello! I'm doing well, thank you for asking."
                    ),
                    finishReason = "stop"
                )
            ),
            usage = IntermittentUsage(
                promptTokens = 20,
                completionTokens = 25,
                totalTokens = 45
            )
        )
        
        assertThat(response.id).isEqualTo("chatcmpl-123")
        assertThat(response.model).isEqualTo("gpt-3.5-turbo")
        assertThat(response.choices).hasSize(1)
        assertThat(response.choices[0].index).isEqualTo(0)
        assertThat(response.choices[0].message?.role).isEqualTo(MessageRole.ASSISTANT)
        assertThat(response.choices[0].message?.content).isEqualTo("Hello! I'm doing well, thank you for asking.")
        assertThat(response.choices[0].finishReason).isEqualTo("stop")
        assertThat(response.usage?.promptTokens).isEqualTo(20)
        assertThat(response.usage?.completionTokens).isEqualTo(25)
        assertThat(response.usage?.totalTokens).isEqualTo(45)
    }
    
    @Test
    fun `should create intermittent error with all fields`() {
        val error = IntermittentError(
            type = "invalid_request_error",
            message = "The request is invalid",
            code = "invalid_request",
            param = "model"
        )
        
        assertThat(error.type).isEqualTo("invalid_request_error")
        assertThat(error.message).isEqualTo("The request is invalid")
        assertThat(error.code).isEqualTo("invalid_request")
        assertThat(error.param).isEqualTo("model")
    }
    
    @Test
    fun `should create streaming events`() {
        val messageStart = IntermittentStreamEvent.MessageStart(
            id = "chatcmpl-123",
            model = "gpt-3.5-turbo"
        )
        
        val messageDelta = IntermittentStreamEvent.MessageDelta(
            delta = IntermittentDelta(
                content = "Hello"
            )
        )
        
        val messageEnd = IntermittentStreamEvent.MessageEnd(
            finishReason = "stop",
            usage = IntermittentUsage(
                promptTokens = 20,
                completionTokens = 25,
                totalTokens = 45
            )
        )
        
        val error = IntermittentStreamEvent.Error(
            error = IntermittentError(
                type = "invalid_request_error",
                message = "The request is invalid"
            )
        )
        
        assertThat(messageStart).isInstanceOf(IntermittentStreamEvent.MessageStart::class.java)
        assertThat(messageDelta).isInstanceOf(IntermittentStreamEvent.MessageDelta::class.java)
        assertThat(messageEnd).isInstanceOf(IntermittentStreamEvent.MessageEnd::class.java)
        assertThat(error).isInstanceOf(IntermittentStreamEvent.Error::class.java)
    }
}
