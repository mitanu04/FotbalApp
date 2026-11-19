package ro.fluierulfinal.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Time rules shared by the whole app. */
object AppTime {

    /** All dates in the app (fixture days, "today") are Romanian dates. */
    val ZONE: ZoneId = ZoneId.of("Europe/Bucharest")

    fun today(now: Instant): LocalDate = now.atZone(ZONE).toLocalDate()

    /** The free API-Football plan serves fixtures only for yesterday, today and tomorrow. */
    fun isInFreePlanWindow(day: LocalDate, now: Instant): Boolean {
        val today = today(now)
        return day >= today.minusDays(1) && day <= today.plusDays(1)
    }
}
