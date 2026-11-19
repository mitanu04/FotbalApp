package ro.fluierulfinal.data.repository

/**
 * Short, safe description of an error for Logcat: the exception type and the first line of its message.
 * Never log the whole exception from Supabase: its message includes the request headers,
 * among them the user's session token (Authorization: Bearer ...).
 */
internal fun Throwable.safeDescription(): String =
    "${this::class.simpleName}: ${message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()}"
