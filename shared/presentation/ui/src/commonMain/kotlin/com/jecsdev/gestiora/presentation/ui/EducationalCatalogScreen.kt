package com.jecsdev.gestiora.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.EducationalArticle
import com.jecsdev.gestiora.presentation.viewmodels.EducationViewModel
import gestiora.shared.presentation.ui.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class CatalogState {
    var query by mutableStateOf("")
    var favoritesOnly by mutableStateOf(false)
    var expandedId by mutableStateOf<String?>(null)
    var articles by mutableStateOf<List<EducationalArticle>>(emptyList())
    var loading by mutableStateOf(true)
    var readFailed by mutableStateOf(false)
    var writeFailed by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var refresh by mutableStateOf(0)
}

@Composable
internal fun EducationalCatalogScreen(viewModel: EducationViewModel, onBack: () -> Unit) {
    val ui = LocalUiCopy.current
    val locale = ui.s(Res.string.catalog_locale)
    val seed = remember(ui) { educationalSeed(ui) }
    val state = retain(locale) { CatalogState() }
    val scope = retainedOperationScope()
    AppointmentBackHandler(true, onBack)
    LaunchedEffect(viewModel, locale, state.query, state.favoritesOnly, state.refresh) {
        state.loading = true
        state.readFailed = false
        try {
            if (state.query.isNotBlank()) delay(250)
            val prepared = viewModel.prepare(locale, BABY_QUESTIONS_VERSION, seed)
            if (prepared.isFailure) {
                state.readFailed = true
            } else {
                viewModel.search(locale, state.query, state.favoritesOnly)
                    .onSuccess { state.articles = it }
                    .onFailure { state.readFailed = true }
            }
        } finally { state.loading = false }
    }
    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                val arrowColor = LocalContentColor.current
                Canvas(Modifier.size(20.dp)) {
                    val center = size.height / 2
                    val tip = size.width * 0.2f
                    val wing = size.width * 0.5f
                    val arrow = Path().apply {
                        moveTo(wing, size.height * 0.2f)
                        lineTo(tip, center)
                        lineTo(wing, size.height * 0.8f)
                    }
                    val strokeWidth = 2.dp.toPx()
                    drawPath(arrow, arrowColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawLine(arrowColor, Offset(tip, center), Offset(size.width * 0.85f, center), strokeWidth, StrokeCap.Round)
                }
                Spacer(Modifier.width(8.dp))
                Text(ui.s(Res.string.back))
            }
            EditorialHeader(ui.s(Res.string.catalog_title), eyebrow = ui.s(Res.string.catalog_offline))
        }
        item {
            Text(ui.s(Res.string.catalog_disclaimer),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            PastelTextField(value = state.query, onValueChange = { if (it.length <= 256) state.query = it },
                label = { Text(ui.s(Res.string.catalog_search)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), enabled = !state.saving,
                trailingIcon = { if (state.query.isNotEmpty()) TextButton(onClick = { state.query = "" }, enabled = !state.saving) {
                    Text(ui.s(Res.string.catalog_clear))
                } })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!state.favoritesOnly, { state.favoritesOnly = false }, enabled = !state.saving,
                    label = { Text(ui.s(Res.string.catalog_all)) })
                FilterChip(state.favoritesOnly, { state.favoritesOnly = true }, enabled = !state.saving,
                    label = { Text(ui.s(Res.string.catalog_saved)) })
            }
        }
        if (state.writeFailed) item {
            Text(ui.s(Res.string.catalog_save_failed), color = MaterialTheme.colorScheme.error)
        }
        when {
            state.loading -> item { CircularProgressIndicator() }
            state.readFailed -> item {
                Text(ui.s(Res.string.catalog_load_failed), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { state.refresh++ }) { Text(ui.s(Res.string.retry)) }
            }
            state.articles.isEmpty() -> item {
                Text(ui.s(if (state.query.isNotBlank()) Res.string.catalog_no_matches else if (state.favoritesOnly) Res.string.catalog_no_saved else Res.string.catalog_no_content))
            }
            else -> items(state.articles, key = { it.id }) { article ->
                CatalogArticleCard(article, state.expandedId == article.id, state.saving,
                    onExpand = { state.expandedId = if (state.expandedId == article.id) null else article.id },
                    onFavorite = {
                        if (!state.saving) {
                            state.saving = true
                            state.writeFailed = false
                            scope.launch {
                                try {
                                    viewModel.setFavorite(article.id, !article.isFavorite)
                                        .onSuccess { state.refresh++ }
                                        .onFailure { state.writeFailed = true }
                                } finally { state.saving = false }
                            }
                        }
                    })
            }
        }
    }
}

@Composable
private fun CatalogArticleCard(article: EducationalArticle, expanded: Boolean, saving: Boolean, onExpand: () -> Unit, onFavorite: () -> Unit) {
    val ui = LocalUiCopy.current
    val duration = if (LocalReducedMotion.current) 0 else 220
    EditorialRecordCard {
        TextButton(onClick = onExpand, modifier = Modifier.fillMaxWidth().semantics {
            stateDescription = ui.s(if (expanded) Res.string.answer_expanded else Res.string.answer_collapsed)
        }) { Text(article.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onFavorite, enabled = !saving, modifier = Modifier.semantics {
            stateDescription = ui.s(if (article.isFavorite) Res.string.catalog_saved_state else Res.string.catalog_unsaved_state)
        }) { Text(ui.s(if (article.isFavorite) Res.string.catalog_remove_saved else Res.string.catalog_save)) }
        AnimatedVisibility(expanded,
            enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
            exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))) {
            Column(if (expanded) Modifier else Modifier.clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(article.body, style = MaterialTheme.typography.bodyLarge)
                Text(ui.s(Res.string.catalog_content_language, ui.s(when (article.locale) {
                    "es" -> Res.string.spanish; "fr" -> Res.string.catalog_french; else -> Res.string.english
                })), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(ui.s(Res.string.catalog_updated, dateLabel(ui, utcMillisToIso(article.updatedAt), true)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(ui.s(Res.string.catalog_sources_checked, dateLabel(ui, article.sourceCheckedOn, true)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                article.reviewedOn?.let { Text(ui.s(Res.string.catalog_reviewed, dateLabel(ui, it, true)), style = MaterialTheme.typography.bodySmall) }
                article.sources.forEach { source ->
                    val uriHandler = LocalUriHandler.current
                    var linkError by remember(source.url) { mutableStateOf(false) }
                    Text(ui.s(Res.string.source, source.institution, source.title), style = MaterialTheme.typography.bodySmall)
                    Text(ui.s(Res.string.summary_adapted_by_gestiora_original_source, ui.s(when (source.language) {
                        "es" -> Res.string.spanish; "fr" -> Res.string.catalog_french; else -> Res.string.english
                    })), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(enabled = expanded, onClick = {
                        linkError = false
                        try { uriHandler.openUri(source.url) } catch (_: IllegalArgumentException) { linkError = true }
                    }) { Text(ui.s(Res.string.read_official_source, source.institution)) }
                    if (linkError) {
                        Text(ui.s(Res.string.we_couldn_t_open_the_link_you_can), color = MaterialTheme.colorScheme.error)
                        SelectionContainer { Text(source.url, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Text(ui.s(Res.string.catalog_disclaimer),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
