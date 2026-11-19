package ro.fluierulfinal.data.remote

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.security.MessageDigest

/**
 * When API-Football has no logo for a team, its media server still answers 200 with a generic
 * picture ("image not available" or "OFFICIAL LOGO SOON"). This interceptor, used only by the
 * image loader (Coil), recognizes those two pictures by their MD5 and turns them into a 404,
 * so the UI shows our own placeholder (the team's initials) instead.
 */
class LogoPlaceholderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!response.isSuccessful || request.url.host != MEDIA_HOST) return response

        // peekBody reads a copy, so the original body stays usable when it is a real logo
        val bytes = response.peekBody(MAX_PEEK_BYTES).bytes()
        if (!isApiPlaceholder(bytes)) return response

        response.close()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("API-Football placeholder logo")
            .body(ByteArray(0).toResponseBody())
            .build()
    }

    companion object {
        const val MEDIA_HOST = "media.api-sports.io"
        private const val MAX_PEEK_BYTES = 512L * 1024

        /** MD5 of the two generic pictures, as served on 19.11.2026. */
        private val PLACEHOLDER_MD5 = setOf(
            "68ac0d5773da5ee81444ade70d89533d",   // "image not available" (29 416 bytes)
            "fbe651acfa5699e1adbb938d7cc728d3",   // "OFFICIAL LOGO SOON" (6 746 bytes)
        )

        fun isApiPlaceholder(bytes: ByteArray): Boolean {
            val md5 = MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }
            return md5 in PLACEHOLDER_MD5
        }
    }
}
