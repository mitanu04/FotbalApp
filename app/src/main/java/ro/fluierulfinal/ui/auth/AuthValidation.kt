package ro.fluierulfinal.ui.auth

import ro.fluierulfinal.data.repository.AuthError

/** Errors shown under each field; null = the field is fine. Texts are UI, so in Romanian. */
data class FieldErrors(
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
) {
    val isValid get() = email == null && password == null && confirmPassword == null
}

/**
 * Client-side checks, done before calling the server (fast feedback, no wasted request).
 * Pure functions, unit tested in AuthValidationTest.
 */
object AuthValidation {

    /** Supabase's default minimum password length. */
    const val MIN_PASSWORD_LENGTH = 6

    // Simple and readable: something@something.domain, no spaces. The server does the strict check.
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun validate(email: String, password: String, confirmPassword: String?, isRegister: Boolean): FieldErrors = FieldErrors(
        email = when {
            email.isBlank() -> "Introdu adresa de email."
            !EMAIL.matches(email.trim()) -> "Adresa de email nu e validă."
            else -> null
        },
        password = when {
            password.isEmpty() -> "Introdu parola."
            isRegister && password.length < MIN_PASSWORD_LENGTH -> "Parola trebuie să aibă cel puțin $MIN_PASSWORD_LENGTH caractere."
            else -> null
        },
        confirmPassword = when {
            !isRegister -> null
            confirmPassword != password -> "Parolele nu coincid."
            else -> null
        },
    )

    /** Message for an error returned by the server. */
    fun messageFor(error: AuthError): String = when (error) {
        AuthError.INVALID_CREDENTIALS -> "Email sau parolă greșită."
        AuthError.EMAIL_NOT_CONFIRMED -> "Contul nu e confirmat încă. Deschide linkul primit pe email."
        AuthError.USER_ALREADY_EXISTS -> "Există deja un cont cu acest email. Încearcă să te autentifici."
        AuthError.WEAK_PASSWORD -> "Parola e prea slabă. Alege una mai lungă, cu litere și cifre."
        AuthError.INVALID_EMAIL -> "Adresa de email nu e validă."
        AuthError.RATE_LIMITED -> "Prea multe încercări. Așteaptă câteva minute și încearcă din nou."
        AuthError.NETWORK -> "Nu ne putem conecta la server. Verifică internetul."
        AuthError.UNKNOWN -> "A apărut o eroare neașteptată. Încearcă din nou."
    }
}
