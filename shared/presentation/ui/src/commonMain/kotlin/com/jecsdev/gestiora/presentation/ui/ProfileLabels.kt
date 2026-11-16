package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

private val countries = mapOf(
    "Argentina" to Res.string.argentina,
    "Bolivia" to Res.string.bolivia,
    "Chile" to Res.string.chile,
    "Colombia" to Res.string.colombia,
    "Ecuador" to Res.string.ecuador,
    "España" to Res.string.spain,
    "Estados Unidos" to Res.string.united_states,
    "México" to Res.string.mexico,
    "Paraguay" to Res.string.paraguay,
    "Perú" to Res.string.peru,
    "Uruguay" to Res.string.uruguay,
    "Venezuela" to Res.string.venezuela,
    "Otro" to Res.string.other
)
internal fun countryLabel(value: String, ui: UiCopy): String = countries[value]?.let { ui.s(it) } ?: value
internal fun languageLabel(value: String, ui: UiCopy): String = when (value) {
    "es", "Español" -> ui.s(Res.string.spanish)
    "en", "English" -> ui.s(Res.string.english)
    "fr", "Français" -> ui.s(Res.string.french_language)
    else -> value
}
