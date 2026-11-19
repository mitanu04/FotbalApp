package ro.fluierulfinal.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validLogin() {
        assertTrue(AuthValidation.validate("mihai@example.com", "x", null, isRegister = false).isValid)
    }

    @Test
    fun emailChecks() {
        assertEquals("Introdu adresa de email.", AuthValidation.validate("  ", "secret1", null, false).email)
        assertNotNull(AuthValidation.validate("mihai@", "secret1", null, false).email)
        assertNotNull(AuthValidation.validate("mihai example@x.com", "secret1", null, false).email)
        assertNull(AuthValidation.validate(" mihai@example.ro ", "secret1", null, false).email) // spaces around are trimmed
    }

    @Test
    fun loginDoesNotCheckPasswordLength_registerDoes() {
        assertNull(AuthValidation.validate("a@b.ro", "123", null, isRegister = false).password)
        assertNotNull(AuthValidation.validate("a@b.ro", "123", "123", isRegister = true).password)
    }

    @Test
    fun registerNeedsMatchingPasswords() {
        val errors = AuthValidation.validate("a@b.ro", "secret1", "secret2", isRegister = true)
        assertEquals("Parolele nu coincid.", errors.confirmPassword)
        assertFalse(errors.isValid)
        assertTrue(AuthValidation.validate("a@b.ro", "secret1", "secret1", isRegister = true).isValid)
    }
}
