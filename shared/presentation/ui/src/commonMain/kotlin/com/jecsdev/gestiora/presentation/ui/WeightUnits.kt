package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import gestiora.shared.presentation.ui.generated.resources.*
import kotlin.math.round

internal enum class WeightUnit { KILOGRAMS, POUNDS }
private const val KILOGRAMS_PER_POUND = 0.45359237
internal fun weightInUnit(kilograms: Double, unit: WeightUnit): Double =
    if (unit == WeightUnit.KILOGRAMS) kilograms else kilograms / KILOGRAMS_PER_POUND
private fun weightNumber(value: Double) = formatVitalNumber(round(value * 10.0) / 10.0)

/** Keep canonical precision when switching units without editing a rounded input. */
internal data class WeightEntry(val unit: WeightUnit, val input: String, val kilograms: Double?) {
    fun edit(input: String) = copy(input = input, kilograms = parseVitalNumber(input)?.let {
        if (unit == WeightUnit.KILOGRAMS) it else it * KILOGRAMS_PER_POUND
    })
    fun switchTo(unit: WeightUnit) = copy(unit = unit,
        input = kilograms?.let { weightNumber(weightInUnit(it, unit)) } ?: input)
    companion object {
        fun fromKilograms(kilograms: Double?, unit: WeightUnit) = WeightEntry(unit,
            kilograms?.let { if (unit == WeightUnit.KILOGRAMS) formatVitalNumber(it) else weightNumber(weightInUnit(it, unit)) }.orEmpty(), kilograms)
    }
}

internal fun weightDisplay(kilograms: Double?, unit: WeightUnit, ui: UiCopy? = null): String {
    val kg = kilograms?.let(::weightNumber) ?: "—"
    val lb = kilograms?.let { weightNumber(weightInUnit(it, WeightUnit.POUNDS)) } ?: "—"
    return if (unit == WeightUnit.KILOGRAMS) ui?.s(Res.string.weight_kg_lb, kg, lb) ?: "$kg kg ($lb lb)"
        else ui?.s(Res.string.weight_lb_kg, lb, kg) ?: "$lb lb ($kg kg)"
}

@Composable
internal fun WeightUnitSelector(unit: WeightUnit, enabled: Boolean = true, onSelect: (WeightUnit) -> Unit) {
    val ui = LocalUiCopy.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WeightUnit.entries.forEach { option ->
            FilterChip(selected = unit == option, enabled = enabled, onClick = { onSelect(option) },
                label = { Text(ui.s(if (option == WeightUnit.KILOGRAMS) Res.string.weight_unit_kg else Res.string.weight_unit_lb)) })
        }
    }
}
