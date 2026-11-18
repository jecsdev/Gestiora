package com.jecsdev.gestiora.data.models

data class EducationalSourceData(val institution: String, val title: String, val url: String, val language: String)
data class EducationalContentData(
    val id: String, val locale: String, val title: String, val body: String, val tags: String,
    val sources: List<EducationalSourceData>, val updatedAt: Long, val sourceCheckedOn: String,
    val reviewedOn: String?, val isFavorite: Boolean = false,
)

/** Explicit ES/EN/FR folding; retain punctuation so SQL wildcard characters stay literal. */
fun educationSearchText(value: String): String = buildString {
    value.lowercase().forEach { char ->
        append(when (char) {
            in '\u0300'..'\u036f' -> ""
            'á', 'à', 'â', 'ä', 'ã', 'å' -> "a"
            'é', 'è', 'ê', 'ë' -> "e"
            'í', 'ì', 'î', 'ï' -> "i"
            'ó', 'ò', 'ô', 'ö', 'õ' -> "o"
            'ú', 'ù', 'û', 'ü' -> "u"
            'ñ' -> "n"
            'ç' -> "c"
            'œ' -> "oe"
            'æ' -> "ae"
            '’' -> "'"
            else -> char.toString()
        })
    }
}.trim().replace(Regex("\\s+"), " ")
