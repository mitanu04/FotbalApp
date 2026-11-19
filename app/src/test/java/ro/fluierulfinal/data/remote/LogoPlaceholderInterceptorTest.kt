package ro.fluierulfinal.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two generic API-Football pictures are recognized; a real logo is not. Files saved from the media server. */
class LogoPlaceholderInterceptorTest {

    private fun bytes(name: String) = requireNotNull(javaClass.classLoader?.getResource("logos/$name")).readBytes()

    @Test
    fun recognizesImageNotAvailable() {
        assertTrue(LogoPlaceholderInterceptor.isApiPlaceholder(bytes("placeholder_image_not_available.png")))
    }

    @Test
    fun recognizesOfficialLogoSoon() {
        assertTrue(LogoPlaceholderInterceptor.isApiPlaceholder(bytes("placeholder_official_logo_soon.png")))
    }

    @Test
    fun realLogoIsKept() {
        assertFalse(LogoPlaceholderInterceptor.isApiPlaceholder(bytes("barcelona_529.png")))
    }
}
