package com.jecsdev.gestiora.presentation.ui

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode

internal actual fun localizedCountryName(code: String, language: String): String =
    NSLocale(localeIdentifier = language).displayNameForKey(NSLocaleCountryCode, code) ?: code
