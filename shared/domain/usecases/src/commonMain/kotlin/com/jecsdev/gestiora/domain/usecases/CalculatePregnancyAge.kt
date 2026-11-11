package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyAge
import com.jecsdev.gestiora.domain.models.PregnancyDating

/** Calendar-day calculation matching the prototype's dating precedence and 280-day estimate. */
class CalculatePregnancyAge {
    operator fun invoke(dating: PregnancyDating, at: String): PregnancyAge? {
        val today = CivilDate.parse(at) ?: return null
        val referenceWeeks = dating.referenceWeeks
        val referenceDays = dating.referenceDays
        if (referenceWeeks != null && referenceWeeks !in 0..42) return null
        if (referenceDays != null && referenceDays !in 0..6) return null

        val reference = CivilDate.parse(dating.referenceDate)
        val referenceStart = if (reference != null && referenceWeeks != null) {
            reference.epochDay - referenceWeeks * 7L - (referenceDays ?: 0)
        } else null
        val lmp = CivilDate.parse(dating.lmp)
        val due = CivilDate.parse(dating.dueDate)
        val start = when {
            dating.method != DatingMethod.FUM && referenceStart != null -> referenceStart
            lmp != null -> lmp.epochDay
            referenceStart != null -> referenceStart
            due != null -> due.epochDay - 280
            else -> return null
        }
        val total = today.epochDay - start
        if (total < Int.MIN_VALUE || total > Int.MAX_VALUE) return null
        val totalDays = total.toInt()
        val weeks = floorDiv(totalDays, 7)
        val days = totalDays - weeks * 7
        val estimatedDueDate = CivilDate.fromEpochDay(start + 280) ?: return null
        return PregnancyAge(
            totalDays = totalDays,
            weeks = weeks,
            days = days,
            trimester = if (weeks < 14) 1 else if (weeks < 28) 2 else 3,
            dueDate = estimatedDueDate.toString(),
            daysLeft = 280 - totalDays,
            progress = (totalDays / 280.0).coerceIn(0.0, 1.0),
        )
    }

    /** The onboarding rule in the React prototype. */
    fun isOngoing(age: PregnancyAge?): Boolean = age != null && age.totalDays in 0..300
}

private fun floorDiv(value: Int, divisor: Int): Int {
    val quotient = value / divisor
    return if (value % divisor < 0) quotient - 1 else quotient
}

/** Proleptic Gregorian civil date; Julian day arithmetic avoids platform date/time APIs. */
private data class CivilDate(val year: Int, val month: Int, val day: Int) {
    val epochDay: Long
        get() {
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            return (day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L) - UNIX_EPOCH_JDN
        }

    override fun toString(): String =
        "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    companion object {
        private const val UNIX_EPOCH_JDN = 2440588L

        fun parse(value: String?): CivilDate? {
            if (value == null || value.length != 10 || value[4] != '-' || value[7] != '-') return null
            if (value.indices.any { it != 4 && it != 7 && value[it] !in '0'..'9' }) return null
            val year = value.substring(0, 4).toInt()
            val month = value.substring(5, 7).toInt()
            val day = value.substring(8, 10).toInt()
            if (year !in 1..9999 || month !in 1..12) return null
            val maxDay = when (month) {
                4, 6, 9, 11 -> 30
                2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
                else -> 31
            }
            return if (day in 1..maxDay) CivilDate(year, month, day) else null
        }

        fun fromEpochDay(epochDay: Long): CivilDate? {
            val jdn = epochDay + UNIX_EPOCH_JDN
            if (jdn !in 1721426L..5373484L) return null // 0001-01-01 through 9999-12-31
            val a = jdn + 32044
            val b = (4 * a + 3) / 146097
            val c = a - (146097 * b) / 4
            val d = (4 * c + 3) / 1461
            val e = c - (1461 * d) / 4
            val m = (5 * e + 2) / 153
            val day = (e - (153 * m + 2) / 5 + 1).toInt()
            val month = (m + 3 - 12 * (m / 10)).toInt()
            val year = (100 * b + d - 4800 + m / 10).toInt()
            return CivilDate(year, month, day)
        }
    }
}
