package com.fatihcure.kolo.providers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fatihcure.kolo.core.DataBufferFactory
import com.fatihcure.kolo.core.DefaultDataBufferFactory
import com.fatihcure.kolo.core.StreamingConfig

/**
 * Configuration for OpenAI provider
 */
data class OpenAIProviderConfig(
    val dataBufferFactory: DataBufferFactory = DefaultDataBufferFactory(),
    val objectMapper: ObjectMapper = ObjectMapper(),
    val streamingConfig: StreamingConfig = StreamingConfig(dataBufferFactory = dataBufferFactory),
) {
    companion object {
        /**
         * Create a default configuration
         */
        fun default(): OpenAIProviderConfig {
            return OpenAIProviderConfig()
        }

        /**
         * Create a configuration with custom data buffer factory
         */
        fun withDataBufferFactory(dataBufferFactory: DataBufferFactory): OpenAIProviderConfig {
            return OpenAIProviderConfig(
                dataBufferFactory = dataBufferFactory,
                streamingConfig = StreamingConfig(dataBufferFactory = dataBufferFactory),
            )
        }

        /**
         * Create a configuration with custom object mapper
         */
        fun withObjectMapper(objectMapper: ObjectMapper): OpenAIProviderConfig {
            return OpenAIProviderConfig(objectMapper = objectMapper)
        }

        /**
         * Create a configuration with custom data buffer factory and object mapper
         */
        fun withCustom(dataBufferFactory: DataBufferFactory, objectMapper: ObjectMapper): OpenAIProviderConfig {
            return OpenAIProviderConfig(
                dataBufferFactory = dataBufferFactory,
                objectMapper = objectMapper,
                streamingConfig = StreamingConfig(dataBufferFactory = dataBufferFactory),
            )
        }
    }
}
