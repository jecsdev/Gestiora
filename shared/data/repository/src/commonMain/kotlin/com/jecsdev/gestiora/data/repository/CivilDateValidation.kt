package com.jecsdev.gestiora.data.repository

internal fun validCivilDate(value: String): Boolean {
    if (value.length != 10 || value[4] != '-' || value[7] != '-' || value.indices.any { it != 4 && it != 7 && value[it] !in '0'..'9' }) return false
    val year = value.substring(0, 4).toInt()
    val month = value.substring(5, 7).toInt()
    val day = value.substring(8, 10).toInt()
    if (year !in 1..9999 || month !in 1..12) return false
    val last = when (month) {
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 31
    }
    return day in 1..last
}
