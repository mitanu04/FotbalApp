package ro.fluierulfinal.data.remote

import kotlinx.serialization.json.Json

/**
 * JSON settings for API-Football responses, shared by Retrofit and the unit tests:
 * - ignoreUnknownKeys: the API sends many fields we don't declare in the DTOs;
 * - coerceInputValues: a `null` sent for a non-null field with a default uses the default instead of crashing;
 * - explicitNulls = false: a missing key is treated like null.
 */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
