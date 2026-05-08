package com.attentive.androidsdk.internal.network

/**
 * Event-API version used by the SDK. Internal/testing use.
 */
enum class ApiVersion {
    /** Legacy callback-based API using query-string parameters (`/e` endpoint). */
    OLD,

    /** Current suspend-based API using JSON bodies via Retrofit (`/mobile` endpoint). */
    NEW,
}
