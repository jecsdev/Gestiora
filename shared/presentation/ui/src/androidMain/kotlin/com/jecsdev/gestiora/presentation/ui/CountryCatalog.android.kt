package com.jecsdev.gestiora.presentation.ui

import java.util.Locale

internal actual fun localizedCountryName(code: String, language: String): String =
    Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.forLanguageTag(language))
