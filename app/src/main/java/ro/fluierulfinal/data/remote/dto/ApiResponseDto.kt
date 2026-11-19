package ro.fluierulfinal.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Envelope shared by every API-Football response:
 * { "get": "...", "errors": [] or {...}, "results": 3, "response": ... }
 *
 * `errors` is an empty list when all is fine and an object with messages otherwise
 * (e.g. {"plan": "Free plans do not have access to this date..."}), even with HTTP 200.
 * That is why it is a raw [JsonElement]; the remote data source turns it into an exception.
 */
@Serializable
data class ApiResponseDto<T>(
    val errors: JsonElement? = null,
    val results: Int = 0,
    val response: T,
)
