package com.example.rawal.domain.calendar

import java.util.Calendar
import java.util.Locale

/**
 * Pure-Kotlin Gregorian + Jalali (Persian Shamsi) date & weekly schedule engine.
 * Compatible with Android API 24+ without requiring java.time desugaring.
 */
data class RawalDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<RawalDate> {

    fun toIso(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    override fun compareTo(other: RawalDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toCalendar(): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }

    /**
     * Returns 0 = Saturday, 1 = Sunday, 2 = Monday, 3 = Tuesday, 4 = Wednesday, 5 = Thursday, 6 = Friday
     */
    fun rawalDayOfWeekIndex(): Int {
        return when (toCalendar().get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun plusDays(days: Int): RawalDate {
        val cal = toCalendar()
        cal.add(Calendar.DAY_OF_MONTH, days)
        return RawalDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun minusDays(days: Int): RawalDate = plusDays(-days)

    /**
     * Returns Saturday of the current week.
     */
    fun weekStartSaturday(): RawalDate = minusDays(rawalDayOfWeekIndex())

    fun weekEndFriday(): RawalDate = weekStartSaturday().plusDays(6)

    fun daysInMonth(): Int {
        val cal = toCalendar()
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun monthStart(): RawalDate = RawalDate(year, month, 1)

    fun plusMonths(delta: Int): RawalDate {
        val cal = toCalendar()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, delta)
        return RawalDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            1
        )
    }

    /**
     * Exact Jalali (Persian Shamsi) date triple: (jy, jm 1..12, jd 1..31)
     */
    fun toJalali(): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        var gy = year - 1600
        var gm = month - 1
        var gd = day - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) gDayNo += gDaysInMonth[i]
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) gDayNo++
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
            jDayNo -= jDaysInMonth[jm]
            jm++
        }
        val jd = jDayNo + 1
        return Triple(jy, jm + 1, jd)
    }

    companion object {
        fun today(): RawalDate {
            val cal = Calendar.getInstance()
            return RawalDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun parseIso(iso: String): RawalDate? {
            val parts = iso.trim().split("-")
            if (parts.size != 3) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (m !in 1..12 || d !in 1..31) return null
            return RawalDate(y, m, d)
        }

        fun daysBetween(startIso: String, endIso: String): Int {
            val s = parseIso(startIso) ?: return 0
            val e = parseIso(endIso) ?: return 0
            val diffMs = e.toCalendar().timeInMillis - s.toCalendar().timeInMillis
            return (diffMs / (24L * 60L * 60L * 1000L)).toInt()
        }

        /**
         * Computes sleep duration in minutes given sleep time and wake-up time.
         * Handles overnight crossing (e.g. sleep 23:30, wake 07:00 -> 450 min).
         */
        fun calculateSleepMinutes(
            wakeHour: Int?,
            wakeMinute: Int?,
            sleepHour: Int?,
            sleepMinute: Int?
        ): Int? {
            if (wakeHour == null || wakeMinute == null || sleepHour == null || sleepMinute == null) {
                return null
            }
            val wakeTotal = wakeHour * 60 + wakeMinute
            val sleepTotal = sleepHour * 60 + sleepMinute
            val diff = if (wakeTotal >= sleepTotal) {
                wakeTotal - sleepTotal
            } else {
                (24 * 60 - sleepTotal) + wakeTotal
            }
            return diff.coerceIn(0, 24 * 60)
        }
    }
}

data class AvailabilityWindow(
    val unavailableStartHour: Int?,
    val unavailableStartMinute: Int?,
    val unavailableEndHour: Int?,
    val unavailableEndMinute: Int?,
    val availableFromHour: Int,
    val availableFromMinute: Int,
    val isSchoolDay: Boolean
)

object ScheduleHelper {
    /**
     * Weekly schedule per Section 9:
     * - Saturday, Sunday, Monday: 11:30 AM – 6:00 PM unavailable.
     * - Tuesday, Wednesday: 6:30 AM – 1:00 PM unavailable.
     * - Thursday, Friday: Available from 7:00 AM until bedtime.
     */
    fun getScheduleForDate(date: RawalDate): AvailabilityWindow {
        return when (date.rawalDayOfWeekIndex()) {
            0, 1, 2 -> AvailabilityWindow(
                unavailableStartHour = 11,
                unavailableStartMinute = 30,
                unavailableEndHour = 18,
                unavailableEndMinute = 0,
                availableFromHour = 7,
                availableFromMinute = 0,
                isSchoolDay = true
            )
            3, 4 -> AvailabilityWindow(
                unavailableStartHour = 6,
                unavailableStartMinute = 30,
                unavailableEndHour = 13,
                unavailableEndMinute = 0,
                availableFromHour = 13,
                availableFromMinute = 0,
                isSchoolDay = true
            )
            else -> AvailabilityWindow(
                unavailableStartHour = null,
                unavailableStartMinute = null,
                unavailableEndHour = null,
                unavailableEndMinute = null,
                availableFromHour = 7,
                availableFromMinute = 0,
                isSchoolDay = false
            )
        }
    }
}
