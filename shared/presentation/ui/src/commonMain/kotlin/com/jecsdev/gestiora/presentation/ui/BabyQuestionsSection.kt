package com.jecsdev.gestiora.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import gestiora.shared.presentation.ui.generated.resources.*

@Composable
internal fun BabyQuestionsSection(week: Int) {
    val ui = LocalUiCopy.current
    var expandedId by retain(week) { mutableStateOf<String?>(null) }
    val duration = if (LocalReducedMotion.current) 0 else 220
    EditorialSection(ui.s(Res.string.questions_about_this_stage)) {
        Text(ui.s(Res.string.explored_week_prepared_educational_answers, week),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        babyQuestionsFor(week).forEach { entry ->
            val expanded = expandedId == entry.id
            EditorialRecordCard {
                TextButton(
                    modifier = Modifier.fillMaxWidth().semantics { stateDescription = ui.s(if (expanded) Res.string.answer_expanded else Res.string.answer_collapsed) },
                    onClick = { expandedId = if (expanded) null else entry.id },
                ) { Text(ui.s(entry.question), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium) }
                AnimatedVisibility(expanded,
                    enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
                    exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))) {
                    Column(if (expanded) Modifier else Modifier.clearAndSetSemantics { }) {
                        EducationalAnswer(entry, enabled = expanded)
                    }
                }
            }
        }
        EducationalSourceNote()
    }
}

@Composable
internal fun EducationalSourceNote() {
    val ui = LocalUiCopy.current
    Text(ui.s(Res.string.sources_checked_on_answers_can_be_read_offline_full, dateLabel(ui, BABY_QUESTIONS_CHECKED_ON)),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun EducationalAnswer(entry: BabyQuestion, enabled: Boolean = true) {
    val ui = LocalUiCopy.current
    val uriHandler = LocalUriHandler.current
    var linkError by remember(entry.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(ui.s(entry.answer), style = MaterialTheme.typography.bodyLarge)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui.s(Res.string.source, ui.s(entry.source.institution), ui.s(entry.source.title)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(ui.s(Res.string.summary_adapted_by_gestiora_original_source, ui.s(entry.source.language)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(enabled = enabled, onClick = {
                linkError = false
                try { uriHandler.openUri(entry.source.url) }
                catch (_: IllegalArgumentException) { linkError = true }
            }) { Text(ui.s(Res.string.read_official_source, ui.s(entry.source.institution))) }
            if (linkError) {
                Text(ui.s(Res.string.we_couldn_t_open_the_link_you_can), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(entry.source.url, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
