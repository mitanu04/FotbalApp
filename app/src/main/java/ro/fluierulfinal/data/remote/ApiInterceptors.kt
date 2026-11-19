package ro.fluierulfinal.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/** Adds the API key header to every request, so no call site has to remember it. */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("x-apisports-key", apiKey)
            .build()
        return chain.proceed(request)
    }
}

/**
 * Logs every API-Football call and how many daily requests are left (header
 * `x-ratelimit-requests-remaining`). Filter Logcat by the tag "ApiQuota" to see exactly when the
 * app really hits the network; a screen served from cache produces no line here.
 */
class RateLimitLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val remaining = response.header("x-ratelimit-requests-remaining") ?: "?"
        val limit = response.header("x-ratelimit-requests-limit") ?: "?"
        Log.i(TAG, "${request.url.encodedPath}?${request.url.encodedQuery ?: ""} -> HTTP ${response.code}, remaining today: $remaining/$limit")
        return response
    }

    companion object {
        const val TAG = "ApiQuota"
    }
}
