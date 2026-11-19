package ro.fluierulfinal.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import ro.fluierulfinal.data.remote.dto.ApiResponseDto

/** An error reported by API-Football (wrong key, daily quota used up, date outside the free plan...). */
class ApiFootballException(message: String) : Exception(message)

/**
 * Returns the response, or throws if the API reported errors.
 * API-Football answers HTTP 200 even on errors and puts the reason in "errors",
 * e.g. {"plan": "Free plans do not have access to this season, try from 2022 to 2024."}.
 */
fun <T> ApiResponseDto<T>.orThrow(): T {
    val e = errors
    if (e is JsonObject && e.isNotEmpty()) {
        val text = e.values.joinToString(" ") { runCatching { it.jsonPrimitive.content }.getOrDefault(it.toString()) }
        throw ApiFootballException(text)
    }
    return response
}
