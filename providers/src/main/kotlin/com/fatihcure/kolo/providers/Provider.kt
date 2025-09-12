package com.fatihcure.kolo.providers

import com.fatihcure.kolo.core.Provider

/**
 * Re-export the Provider interface from the core module for convenience
 */
typealias Provider<RequestType, ResponseType> = com.fatihcure.kolo.core.Provider<RequestType, ResponseType>
