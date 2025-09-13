package com.fatihcure.kolo.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.io.Reader

/**
 * JSON serialization/deserialization utilities for Kolo's intermittent format
 */
object JsonSerde {

    val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    /**
     * Serializes an IntermittentRequest to JSON string
     */
    fun toJson(request: IntermittentRequest): String {
        return objectMapper.writeValueAsString(request)
    }

    /**
     * Deserializes a JSON string to IntermittentRequest
     */
    fun fromJson(json: String): IntermittentRequest {
        return objectMapper.readValue(json)
    }

    /**
     * Deserializes a JSON string from InputStream to IntermittentRequest
     */
    fun fromJson(inputStream: InputStream): IntermittentRequest {
        return objectMapper.readValue(inputStream)
    }

    /**
     * Deserializes a JSON string from Reader to IntermittentRequest
     */
    fun fromJson(reader: Reader): IntermittentRequest {
        return objectMapper.readValue(reader)
    }

    /**
     * Serializes an IntermittentResponse to JSON string
     */
    fun toJson(response: IntermittentResponse): String {
        return objectMapper.writeValueAsString(response)
    }

    /**
     * Deserializes a JSON string to IntermittentResponse
     */
    fun responseFromJson(json: String): IntermittentResponse {
        return objectMapper.readValue(json)
    }

    /**
     * Deserializes a JSON string from InputStream to IntermittentResponse
     */
    fun responseFromJson(inputStream: InputStream): IntermittentResponse {
        return objectMapper.readValue(inputStream)
    }

    /**
     * Deserializes a JSON string from Reader to IntermittentResponse
     */
    fun responseFromJson(reader: Reader): IntermittentResponse {
        return objectMapper.readValue(reader)
    }

    /**
     * Serializes an IntermittentStreamEvent to JSON string
     */
    fun toJson(streamEvent: IntermittentStreamEvent): String {
        return objectMapper.writeValueAsString(streamEvent)
    }

    /**
     * Deserializes a JSON string to IntermittentStreamEvent
     */
    fun streamEventFromJson(json: String): IntermittentStreamEvent {
        return objectMapper.readValue(json)
    }

    /**
     * Deserializes a JSON string from InputStream to IntermittentStreamEvent
     */
    fun streamEventFromJson(inputStream: InputStream): IntermittentStreamEvent {
        return objectMapper.readValue(inputStream)
    }

    /**
     * Deserializes a JSON string from Reader to IntermittentStreamEvent
     */
    fun streamEventFromJson(reader: Reader): IntermittentStreamEvent {
        return objectMapper.readValue(reader)
    }

    /**
     * Serializes an IntermittentError to JSON string
     */
    fun toJson(error: IntermittentError): String {
        return objectMapper.writeValueAsString(error)
    }

    /**
     * Deserializes a JSON string to IntermittentError
     */
    fun errorFromJson(json: String): IntermittentError {
        return objectMapper.readValue(json)
    }

    /**
     * Deserializes a JSON string from InputStream to IntermittentError
     */
    fun errorFromJson(inputStream: InputStream): IntermittentError {
        return objectMapper.readValue(inputStream)
    }

    /**
     * Deserializes a JSON string from Reader to IntermittentError
     */
    fun errorFromJson(reader: Reader): IntermittentError {
        return objectMapper.readValue(reader)
    }

    /**
     * Serializes any object to JSON string
     */
    fun <T> toJson(obj: T): String {
        return objectMapper.writeValueAsString(obj)
    }

    /**
     * Deserializes a JSON string to any type
     */
    inline fun <reified T> fromJson(json: String): T {
        return objectMapper.readValue(json)
    }

    /**
     * Deserializes a JSON string from InputStream to any type
     */
    inline fun <reified T> fromJson(inputStream: InputStream): T {
        return objectMapper.readValue(inputStream)
    }

    /**
     * Deserializes a JSON string from Reader to any type
     */
    inline fun <reified T> fromJson(reader: Reader): T {
        return objectMapper.readValue(reader)
    }

    /**
     * Deserializes a JSON string to any type using Class
     */
    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }

    /**
     * Deserializes a JSON string from InputStream to any type using Class
     */
    fun <T> fromJson(inputStream: InputStream, clazz: Class<T>): T {
        return objectMapper.readValue(inputStream, clazz)
    }

    /**
     * Deserializes a JSON string from Reader to any type using Class
     */
    fun <T> fromJson(reader: Reader, clazz: Class<T>): T {
        return objectMapper.readValue(reader, clazz)
    }
}
