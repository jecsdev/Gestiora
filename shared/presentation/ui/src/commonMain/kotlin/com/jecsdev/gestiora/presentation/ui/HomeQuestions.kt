package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gestiora.shared.presentation.ui.generated.resources.*

/** Reading controls the card: there is no timer or auto-rotation. */
internal fun homeQuestions(): List<BabyQuestion> = babyQuestions.filter {
    it.id == "prenatal-contacts" || it.id == "early-ultrasound"
}

@Composable
internal fun HomeQuestions() {
    val ui = LocalUiCopy.current
    val questions = remember { homeQuestions() }
    var index by retain { mutableIntStateOf(0) }
    val entry = questions[index.coerceIn(questions.indices)]
    EditorialSection(ui.s(Res.string.home_faq_title)) {
        Text(ui.s(Res.string.official_guidance), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        key(entry.id) {
            Box(Modifier.screenEntrance(8.dp)) {
                EditorialRecordCard {
                    Text(ui.s(entry.question), style = MaterialTheme.typography.titleMedium)
                    EducationalAnswer(entry)
                }
            }
        }
        Text(ui.s(Res.string.question_position, index + 1, questions.size), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(enabled = index > 0, onClick = { index-- }) { Text(ui.s(Res.string.previous)) }
            OutlinedButton(enabled = index < questions.lastIndex, onClick = { index++ }) { Text(ui.s(Res.string.next)) }
        }
        EducationalSourceNote()
    }
}
