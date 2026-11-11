package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.models.PregnancyProfile

class CorruptProfileException : IllegalStateException("Stored pregnancy profile is corrupt")
class UnsupportedProfileVersionException : IllegalStateException("Stored pregnancy profile version is unsupported")

/** Versioned, length-prefixed format. A field may contain any character, including line breaks. */
internal object ProfileCodec {
    private const val HEADER = "GESTIORA_PROFILE:1\n"
    private const val PREFIX = "GESTIORA_PROFILE:"
    private const val MAX_FIELD_LENGTH = 16_384

    fun encode(profile: PregnancyProfile): String = buildString {
        require(profile.country.isNotBlank() && profile.language.isNotBlank()) { "Profile country and language are required" }
        require(profile.dating.referenceWeeks == null || profile.dating.referenceWeeks in 0..42)
        require(profile.dating.referenceDays == null || profile.dating.referenceDays in 0..6)
        append(HEADER)
        listOf(
            profile.motherName,
            profile.babyName,
            profile.country,
            profile.language,
            profile.dating.method.name,
            profile.dating.lmp,
            profile.dating.dueDate,
            profile.dating.referenceDate,
            profile.dating.referenceWeeks?.toString(),
            profile.dating.referenceDays?.toString(),
        ).forEach { value ->
            if (value == null) append("-1:")
            else {
                require(value.length <= MAX_FIELD_LENGTH) { "Profile field is too long" }
                append(value.length).append(':').append(value)
            }
        }
    }

    fun decode(raw: String): PregnancyProfile {
        if (!raw.startsWith(PREFIX)) throw CorruptProfileException()
        if (!raw.startsWith(HEADER)) throw UnsupportedProfileVersionException()
        val cursor = Cursor(raw, HEADER.length)
        val motherName = cursor.field()
        val babyName = cursor.field()
        val country = cursor.field() ?: throw CorruptProfileException()
        val language = cursor.field() ?: throw CorruptProfileException()
        val methodName = cursor.field() ?: throw CorruptProfileException()
        val method = DatingMethod.entries.firstOrNull { it.name == methodName } ?: throw CorruptProfileException()
        val lmp = cursor.field()
        val dueDate = cursor.field()
        val referenceDate = cursor.field()
        val referenceWeeks = cursor.field()?.toIntOrNullStrict()
        val referenceDays = cursor.field()?.toIntOrNullStrict()
        if (!cursor.atEnd() || country.isBlank() || language.isBlank()) throw CorruptProfileException()
        if (referenceWeeks != null && referenceWeeks !in 0..42) throw CorruptProfileException()
        if (referenceDays != null && referenceDays !in 0..6) throw CorruptProfileException()
        return PregnancyProfile(
            motherName = motherName,
            babyName = babyName,
            country = country,
            language = language,
            dating = PregnancyDating(method, lmp, dueDate, referenceDate, referenceWeeks, referenceDays),
        )
    }

    private fun String.toIntOrNullStrict(): Int = toIntOrNull() ?: throw CorruptProfileException()

    private class Cursor(private val raw: String, private var position: Int) {
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..6) throw CorruptProfileException()
            val lengthText = raw.substring(position, colon)
            if (lengthText == "-1") {
                position = colon + 1
                return null
            }
            if (lengthText.any { it !in '0'..'9' }) throw CorruptProfileException()
            val length = lengthText.toIntOrNull() ?: throw CorruptProfileException()
            if (length > MAX_FIELD_LENGTH || colon + 1 + length > raw.length) throw CorruptProfileException()
            val value = raw.substring(colon + 1, colon + 1 + length)
            position = colon + 1 + length
            return value
        }

        fun atEnd(): Boolean = position == raw.length
    }
}
