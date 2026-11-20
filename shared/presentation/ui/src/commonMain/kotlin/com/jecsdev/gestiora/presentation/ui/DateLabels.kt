package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

private val shortMonths = listOf(Res.string.month_short_1, Res.string.month_short_2, Res.string.month_short_3, Res.string.month_short_4, Res.string.month_short_5, Res.string.month_short_6, Res.string.month_short_7, Res.string.month_short_8, Res.string.month_short_9, Res.string.month_short_10, Res.string.month_short_11, Res.string.month_short_12)
private val fullMonths = listOf(Res.string.date_month_1, Res.string.date_month_2, Res.string.date_month_3, Res.string.date_month_4, Res.string.date_month_5, Res.string.date_month_6, Res.string.date_month_7, Res.string.date_month_8, Res.string.date_month_9, Res.string.date_month_10, Res.string.date_month_11, Res.string.date_month_12)

/** Presentation only. Storage and calendar arithmetic keep ISO civil dates. */
internal fun dateLabel(ui: UiCopy, value: String?, expanded: Boolean = false): String {
    if (value.isNullOrEmpty()) return "—"
    if (isoToUtcMillis(value) == null) return ui.s(Res.string.invalid_date)
    val day = value.substring(8).toInt()
    val month = value.substring(5, 7).toInt() - 1
    val year = value.substring(0, 4)
    return ui.s(if (expanded) Res.string.date_long else Res.string.date_short,
        day, ui.s((if (expanded) fullMonths else shortMonths)[month]), year)
}
