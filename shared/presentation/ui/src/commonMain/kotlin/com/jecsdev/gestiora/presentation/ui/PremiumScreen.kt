package com.jecsdev.gestiora.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource

private enum class PremiumBenefit(
    val title: StringResource,
    val detail: StringResource,
    val icon: DrawableResource,
) {
    History(Res.string.premium_history, Res.string.premium_history_detail, Res.drawable.premium_history),
    Reports(Res.string.premium_reports, Res.string.premium_reports_detail, Res.drawable.premium_report),
    Statistics(Res.string.premium_statistics, Res.string.premium_statistics_detail, Res.drawable.premium_chart),
    Journal(Res.string.premium_journal, Res.string.premium_journal_detail, Res.drawable.premium_journal),
    AdFree(Res.string.premium_ad_free, Res.string.premium_ad_free_detail, Res.drawable.premium_sparkles),
}

private enum class PremiumPlan(val title: StringResource) {
    Monthly(Res.string.premium_monthly),
    Upfront(Res.string.premium_upfront),
}

private const val MIN_PREMIUM_MONTHS = 1
private const val MAX_PREMIUM_MONTHS = 9
private const val UPFRONT_REFERENCE_MONTHS = 6
private const val UPFRONT_REFERENCE_CENTS = 2000
private const val MONTHLY_PRICE_CENTS = 399

@Composable
internal fun PremiumScreen(
    onBack: () -> Unit,
    onPlans: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    onHistory: (() -> Unit)? = null,
) {
    val ui = LocalUiCopy.current
    var expanded by retain { mutableStateOf<PremiumBenefit?>(PremiumBenefit.History) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenBackButton(onBack)
        PastelHero {
            EditorialStatus(ui.s(Res.string.premium_planned))
            EditorialHeader(
                title = ui.s(Res.string.settings_premium_title),
                supporting = ui.s(Res.string.premium_intro),
            )
        }
        onHistory?.let { OpenPrenatalHistoryCard(it) }
        EditorialSection(ui.s(Res.string.premium_benefits_title)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PremiumBenefit.entries.forEach { benefit ->
                        if (benefit != PremiumBenefit.History) HorizontalDivider()
                        PremiumBenefitRow(benefit, expanded == benefit) {
                            expanded = if (expanded == benefit) null else benefit
                        }
                    }
                }
            }
        }
        GradientButton(onClick = onPlans, modifier = Modifier.fillMaxWidth()) {
            Text(ui.s(Res.string.premium_view_plans))
        }
        Text(
            ui.s(Res.string.premium_free_features),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PremiumBenefitRow(benefit: PremiumBenefit, expanded: Boolean, onClick: () -> Unit) {
    val ui = LocalUiCopy.current
    val color = MaterialTheme.colorScheme.primary
    val duration = if (LocalReducedMotion.current) 0 else 220
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(duration),
        label = "Premium benefit chevron",
    )
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = ui.s(if (expanded) Res.string.premium_collapse else Res.string.premium_expand),
                onClick = onClick,
            ).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PremiumIcon(benefit.icon)
        Text(ui.s(benefit.title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.size(20.dp).graphicsLayer { rotationZ = chevronRotation }) {
            val left = Offset(size.width * 0.22f, size.height * 0.36f)
            val middle = Offset(size.width * 0.5f, size.height * 0.64f)
            val right = Offset(size.width * 0.78f, left.y)
            drawLine(color, left, middle, cap = StrokeCap.Round, strokeWidth = 2.dp.toPx())
            drawLine(color, middle, right, cap = StrokeCap.Round, strokeWidth = 2.dp.toPx())
        }
    }
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(duration)) + expandVertically(tween(duration), expandFrom = Alignment.Top),
        exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration), shrinkTowards = Alignment.Top),
    ) {
        Text(
            ui.s(benefit.detail),
            modifier = (if (expanded) Modifier else Modifier.clearAndSetSemantics {})
                .padding(start = 56.dp, end = 8.dp, bottom = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PremiumIcon(icon: DrawableResource) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun PremiumRecordsBanner(onPlans: () -> Unit, onHistory: (() -> Unit)? = null) {
    if (onHistory != null) {
        OpenPrenatalHistoryCard(onHistory)
        return
    }
    val ui = LocalUiCopy.current
    PastelHero {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumIcon(Res.drawable.premium_sparkles)
            Text(ui.s(Res.string.premium_records_label), style = MaterialTheme.typography.labelLarge)
        }
        EditorialHeader(
            title = ui.s(Res.string.premium_history),
            supporting = ui.s(Res.string.premium_records_summary),
            compact = true,
        )
        GradientButton(onClick = onPlans, modifier = Modifier.fillMaxWidth()) {
            Text(ui.s(Res.string.premium_records_view_plans))
        }
    }
}

@Composable
private fun OpenPrenatalHistoryCard(onHistory: () -> Unit) {
    val ui = LocalUiCopy.current
    PastelHero {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumIcon(Res.drawable.premium_history)
            Text(ui.s(Res.string.premium_history_preview), style = MaterialTheme.typography.labelLarge)
        }
        EditorialHeader(
            title = ui.s(Res.string.premium_history),
            supporting = ui.s(Res.string.premium_history_timeline_summary),
            compact = true,
        )
        GradientButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
            Text(ui.s(Res.string.premium_history_open))
        }
    }
}

@Composable
internal fun PremiumPlansScreen(onBack: () -> Unit) {
    PremiumPlansContent(onBack, initialPlan = PremiumPlan.Monthly)
}

@Composable
private fun PremiumPlansContent(onBack: () -> Unit, initialPlan: PremiumPlan) {
    val ui = LocalUiCopy.current
    var selected by retain { mutableStateOf(initialPlan) }
    var months by retain { mutableIntStateOf(UPFRONT_REFERENCE_MONTHS) }
    val upfrontTotal = (UPFRONT_REFERENCE_CENTS * months + UPFRONT_REFERENCE_MONTHS / 2) / UPFRONT_REFERENCE_MONTHS
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenBackButton(onBack)
        EditorialHeader(ui.s(Res.string.premium_plans), ui.s(Res.string.premium_price_example))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumPlan.entries.forEach { plan ->
                val isSelected = plan == selected
                val colors = MaterialTheme.colorScheme
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) colors.primaryContainer else colors.surface,
                    contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                    border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.outlineVariant),
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = isSelected, role = Role.RadioButton, onClick = { selected = plan },
                            ).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(ui.s(plan.title), style = MaterialTheme.typography.titleMedium)
                                val price = when (plan) {
                                    PremiumPlan.Monthly -> ui.s(Res.string.premium_monthly_price, premiumUsd(ui, MONTHLY_PRICE_CENTS))
                                    PremiumPlan.Upfront -> premiumUsd(ui, upfrontTotal)
                                }
                                Text(price, style = MaterialTheme.typography.titleLarge)
                                if (plan == PremiumPlan.Upfront) {
                                    Text(premiumMonths(ui, months), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (isSelected && plan == PremiumPlan.Upfront) {
                            UpfrontPaymentCalculator(
                                months = months,
                                onMonthsChange = { months = it },
                                savings = MONTHLY_PRICE_CENTS * months - upfrontTotal,
                            )
                        }
                    }
                }
            }
        }
        if (selected == PremiumPlan.Monthly) {
            EditorialRecordCard {
                Text(ui.s(Res.string.premium_plan_detail), style = MaterialTheme.typography.titleMedium)
                Text(
                    ui.s(Res.string.premium_monthly_detail, premiumUsd(ui, MONTHLY_PRICE_CENTS)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            ui.s(Res.string.premium_purchases_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpfrontPaymentCalculator(months: Int, onMonthsChange: (Int) -> Unit, savings: Int) {
    val ui = LocalUiCopy.current
    val label = ui.s(Res.string.premium_upfront_months)
    val monthLabel = premiumMonths(ui, months)
    Column(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider()
        Text(label, style = MaterialTheme.typography.titleMedium)
        Column {
            PastelStepSlider(
                value = months,
                range = MIN_PREMIUM_MONTHS..MAX_PREMIUM_MONTHS,
                label = label,
                valueLabel = monthLabel,
                onValueChange = onMonthsChange,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(premiumMonths(ui, MIN_PREMIUM_MONTHS), style = MaterialTheme.typography.labelMedium)
                Text(premiumMonths(ui, MAX_PREMIUM_MONTHS), style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            ui.s(Res.string.premium_upfront_savings, premiumUsd(ui, savings)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(ui.s(Res.string.premium_upfront_detail), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun premiumUsd(ui: UiCopy, cents: Int): String =
    ui.s(Res.string.premium_usd_amount, cents / 100, (cents % 100).toString().padStart(2, '0'))

private fun premiumMonths(ui: UiCopy, months: Int): String =
    if (months == 1) ui.s(Res.string.premium_one_month)
    else ui.s(Res.string.premium_month_count, months)

@Preview(name = "Premium · benefits · dark", widthDp = 384, heightDp = 820, locale = "es")
@Composable
private fun PremiumPreview() {
    PreviewContent(dark = true) { PremiumScreen(onBack = {}, onPlans = {}) }
}

@Preview(name = "Premium · plans · light", widthDp = 360, heightDp = 760, locale = "en")
@Composable
private fun PremiumPlansPreview() {
    PreviewContent(dark = false) { PremiumPlansScreen(onBack = {}) }
}

@Preview(name = "Premium · upfront calculator · light", widthDp = 320, heightDp = 800, locale = "es")
@Preview(name = "Premium · upfront calculator · dark", widthDp = 320, heightDp = 800, locale = "es", uiMode = 0x20)
@Composable
private fun PremiumUpfrontCalculatorPreview() {
    PreviewContent(dark = isSystemInDarkTheme()) {
        PremiumPlansContent(onBack = {}, initialPlan = PremiumPlan.Upfront)
    }
}

@Preview(name = "Premium · records banner · light", widthDp = 320, locale = "es")
@Preview(name = "Premium · records banner · dark", widthDp = 320, locale = "es", uiMode = 0x20)
@Composable
private fun PremiumRecordsBannerPreview() {
    PreviewContent(dark = isSystemInDarkTheme()) {
        Column(Modifier.padding(24.dp)) { PremiumRecordsBanner(onPlans = {}) }
    }
}
