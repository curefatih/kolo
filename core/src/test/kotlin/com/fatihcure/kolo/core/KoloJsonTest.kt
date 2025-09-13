package com.fatihcure.kolo.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoloJsonTest {

    @Test
    fun `should convert request from JSON to target format`() {
        // Mock normalizer and transformer
        val normalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest {
                return IntermittentRequest(
                    messages = listOf(IntermittentMessage(role = MessageRole.USER, content = request)),
                    model = "gpt-4",
                )
            }

            override fun normalizeResponse(response: String): IntermittentResponse {
                return IntermittentResponse(
                    id = "test",
                    model = "gpt-4",
                    choices = listOf(IntermittentChoice(0, IntermittentMessage(role = MessageRole.ASSISTANT, content = response))),
                )
            }

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError("test", error)
            }
        }

        val transformer = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String {
                return "transformed: ${request.messages.first().content}"
            }

            override fun transformResponse(response: IntermittentResponse): String {
                return "response: ${response.choices.first().message?.content}"
            }

            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun transformError(error: IntermittentError): String {
                return "error: ${error.message}"
            }
        }

        val kolo = Kolo(normalizer, transformer)

        val jsonRequest = """
        "Hello from JSON"
        """.trimIndent()

        val result = kolo.convertRequestFromJson<String>(jsonRequest)

        assertThat(result).isEqualTo("transformed: Hello from JSON")
    }

    @Test
    fun `should convert request from source format to JSON`() {
        val normalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest {
                return IntermittentRequest(
                    messages = listOf(IntermittentMessage(role = MessageRole.USER, content = request)),
                    model = "gpt-4",
                )
            }

            override fun normalizeResponse(response: String): IntermittentResponse {
                return IntermittentResponse(
                    id = "test",
                    model = "gpt-4",
                    choices = listOf(IntermittentChoice(0, IntermittentMessage(role = MessageRole.ASSISTANT, content = response))),
                )
            }

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError("test", error)
            }
        }

        val transformer = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String {
                return "transformed: ${request.messages.first().content}"
            }

            override fun transformResponse(response: IntermittentResponse): String {
                return "response: ${response.choices.first().message?.content}"
            }

            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun transformError(error: IntermittentError): String {
                return "error: ${error.message}"
            }
        }

        val kolo = Kolo(normalizer, transformer)

        val sourceRequest = "Hello from source"
        val jsonResult = kolo.convertRequestToJson(sourceRequest)

        // Parse the JSON to verify it contains the expected structure
        val parsedRequest = kolo.objectMapper.readValue(jsonResult, String::class.java)
        assertThat(parsedRequest).isEqualTo("Hello from source")
    }

    @Test
    fun `should convert response from target format to JSON in BidirectionalKolo`() {
        val sourceNormalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest {
                return IntermittentRequest(
                    messages = listOf(IntermittentMessage(role = MessageRole.USER, content = request)),
                    model = "gpt-4",
                )
            }

            override fun normalizeResponse(response: String): IntermittentResponse {
                return IntermittentResponse(
                    id = "test",
                    model = "gpt-4",
                    choices = listOf(IntermittentChoice(0, IntermittentMessage(role = MessageRole.ASSISTANT, content = response))),
                )
            }

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError("test", error)
            }
        }

        val targetNormalizer = object : Normalizer<String> {
            override fun normalizeRequest(request: String): IntermittentRequest {
                return IntermittentRequest(
                    messages = listOf(IntermittentMessage(role = MessageRole.USER, content = request)),
                    model = "gpt-4",
                )
            }

            override fun normalizeResponse(response: String): IntermittentResponse {
                return IntermittentResponse(
                    id = "test",
                    model = "gpt-4",
                    choices = listOf(IntermittentChoice(0, IntermittentMessage(role = MessageRole.ASSISTANT, content = response))),
                )
            }

            override fun normalizeStreamingResponse(stream: kotlinx.coroutines.flow.Flow<String>): kotlinx.coroutines.flow.Flow<IntermittentStreamEvent> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun normalizeError(error: String): IntermittentError {
                return IntermittentError("test", error)
            }
        }

        val sourceTransformer = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String {
                return "transformed: ${request.messages.first().content}"
            }

            override fun transformResponse(response: IntermittentResponse): String {
                return "response: ${response.choices.first().message?.content}"
            }

            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun transformError(error: IntermittentError): String {
                return "error: ${error.message}"
            }
        }

        val targetTransformer = object : Transformer<String> {
            override fun transformRequest(request: IntermittentRequest): String {
                return "transformed: ${request.messages.first().content}"
            }

            override fun transformResponse(response: IntermittentResponse): String {
                return "response: ${response.choices.first().message?.content}"
            }

            override fun transformStreamingResponse(stream: kotlinx.coroutines.flow.Flow<IntermittentStreamEvent>): kotlinx.coroutines.flow.Flow<String> {
                return kotlinx.coroutines.flow.flowOf()
            }

            override fun transformError(error: IntermittentError): String {
                return "error: ${error.message}"
            }
        }

        val bidirectionalKolo = BidirectionalKolo(sourceNormalizer, targetNormalizer, sourceTransformer, targetTransformer)

        val targetResponse = "Hello from target"
        val jsonResult = bidirectionalKolo.convertResponseToJson(targetResponse)

        // Parse the JSON to verify it contains the expected structure
        val parsedResponse = bidirectionalKolo.objectMapper.readValue(jsonResult, String::class.java)
        assertThat(parsedResponse).isEqualTo("Hello from target")
    }
}
