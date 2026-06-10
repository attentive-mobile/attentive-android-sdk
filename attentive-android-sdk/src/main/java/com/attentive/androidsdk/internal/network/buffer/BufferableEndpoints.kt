package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import okhttp3.Request

@RestrictTo(RestrictTo.Scope.LIBRARY)
object BufferableEndpoints {
    private val PATH_SUFFIXES: Set<String> =
        setOf(
            "/e",
            "/mobile",
            "/token",
            "/mtctrl",
            "/user-update",
            "/opt-in-subscriptions",
            "/opt-out-subscriptions",
        )

    fun shouldBuffer(request: Request): Boolean {
        if (!request.method.equals("POST", ignoreCase = true)) return false
        val path = request.url.encodedPath
        return PATH_SUFFIXES.any { path.endsWith(it) }
    }
}
