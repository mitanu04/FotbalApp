package ro.fluierulfinal.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Time rules shared by the whole app. */
object AppTime {

    /** All dates in the app (fixture days, "today") are Romanian dates. */
    val ZONE: ZoneId = ZoneId.of("Europe/Bucharest")

    fun today(now: Instant): LocalDate = now.atZone(ZONE).toLocalDate()

    /**
     * Days (relative to today) for which our API-Football plan serves fixtures.
     * Free plan: yesterday..tomorrow. With the Pro plan, widen it here (e.g. -7..7) and the whole app follows:
     * Explorează enables those day chips and the repository fetches them.
     */
    val FREE_PLAN_DATE_WINDOW: IntRange = -1..1

    fun isInFreePlanWindow(day: LocalDate, now: Instant): Boolean = isInFreePlanWindow(day, today(now))

    fun isInFreePlanWindow(day: LocalDate, today: LocalDate): Boolean =
        (day.toEpochDay() - today.toEpochDay()) in FREE_PLAN_DATE_WINDOW.first.toLong()..FREE_PLAN_DATE_WINDOW.last.toLong()
}
