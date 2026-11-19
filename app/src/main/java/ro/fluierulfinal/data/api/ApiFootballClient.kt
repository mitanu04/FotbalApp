package ro.fluierulfinal.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** O eroare pe care o putem arăta utilizatorului (ex. „fără internet”, „s-a terminat cota zilnică”). */
class ApiException(message: String) : Exception(message)

/**
 * Vorbește cu API-Football (https://www.api-football.com/documentation-v3).
 * Știe doar să facă cereri HTTP și să întoarcă textul JSON; transformarea în clasele
 * aplicației o face [FixtureParser], ca să poată fi testată fără rețea.
 */
class ApiFootballClient(private val apiKey: String) {

    /**
     * Toate meciurile dintr-o zi, din toată lumea, într-o singură cerere.
     * `suspend` + Dispatchers.IO = cererea rulează pe un fir de fundal, nu blochează ecranul.
     */
    suspend fun fixturesOn(date: LocalDate): String =
        get("/fixtures?date=$date&timezone=$TIMEZONE")

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw ApiException("Lipsește cheia API (API_FOOTBALL_KEY în local.properties).")

        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("x-apisports-key", apiKey)
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw ApiException("Serverul API a răspuns cu codul $code.")
            val body = conn.inputStream.bufferedReader().use { it.readText() }

            // API-Football întoarce 200 și la erori (cheie greșită, cotă depășită, dată în afara planului);
            // eroarea e în câmpul "errors", care e fie listă goală, fie obiect cu mesaje.
            val errors = JSONObject(body).opt("errors")
            if (errors is JSONObject && errors.length() > 0) {
                val messages = errors.keys().asSequence().map { errors.getString(it) }
                throw ApiException(messages.joinToString(" "))
            }
            body
        } catch (e: IOException) {
            throw ApiException("Nu ne putem conecta la API. Verifică internetul.")
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val BASE_URL = "https://v3.football.api-sports.io"
        private const val TIMEZONE = "Europe/Bucharest"
    }
}
