package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel

private val countries = listOf("Argentina", "Bolivia", "Chile", "Colombia", "Ecuador", "España", "Estados Unidos", "México", "Paraguay", "Perú", "Uruguay", "Venezuela", "Otro")

@Composable
fun OnboardingScreen(
    initialProfile: PregnancyProfile?,
    viewModel: ProfileViewModel,
    saving: Boolean,
    saveFailed: Boolean,
    onSave: (PregnancyProfile) -> Unit,
    onCancel: () -> Unit = {},
    cancelRequested: Boolean = false,
    onCancelRequestHandled: () -> Unit = {},
) {
    val ui = LocalUiCopy.current
    val deviceLanguage = androidx.compose.ui.text.intl.Locale.current.language.let { if (it in listOf("es", "en", "fr")) it else "en" }
    var step by retain(initialProfile) { mutableIntStateOf(if (initialProfile == null) 0 else 1) }
    var motherName by retain(initialProfile) { mutableStateOf(initialProfile?.motherName.orEmpty()) }
    var babyName by retain(initialProfile) { mutableStateOf(initialProfile?.babyName.orEmpty()) }
    var country by retain(initialProfile) { mutableStateOf(initialProfile?.country.orEmpty()) }
    var method by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.method ?: DatingMethod.FUM) }
    var lmp by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.lmp.orEmpty()) }
    var dueDate by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.dueDate.orEmpty()) }
    var referenceDate by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.referenceDate ?: todayIsoDate()) }
    var referenceWeeks by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.referenceWeeks?.toString().orEmpty()) }
    var referenceDays by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.referenceDays?.toString() ?: "0") }
    var error by retain { mutableStateOf<org.jetbrains.compose.resources.StringResource?>(null) }
    var discardOpen by retain(initialProfile) { mutableStateOf(false) }
    LaunchedEffect(cancelRequested) {
        if (cancelRequested) {
            if (!saving && initialProfile != null) discardOpen = true
            onCancelRequestHandled()
        }
    }

    fun back() {
        when (onboardingBackAction(step, initialProfile != null, saving, discardOpen)) {
            OnboardingBackAction.BLOCK, OnboardingBackAction.SYSTEM -> Unit
            OnboardingBackAction.DISMISS_DISCARD -> discardOpen = false
            OnboardingBackAction.PREVIOUS_STEP -> { step--; error = null }
            OnboardingBackAction.CONFIRM_DISCARD -> discardOpen = true
        }
    }
    AppointmentBackHandler(saving || step > 0 || initialProfile != null || discardOpen, ::back)
    if (discardOpen) AlertDialog(
        onDismissRequest = { if (!saving) discardOpen = false },
        title = { Text(ui.s(Res.string.discard_profile_changes)) },
        text = { Text(ui.s(Res.string.changes_you_haven_t_saved_will_be_lost)) },
        confirmButton = { TextButton(enabled = !saving, onClick = { discardOpen = false; onCancel() }) { Text(ui.s(Res.string.discard)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = { discardOpen = false }) { Text(ui.s(Res.string.keep_editing)) } },
    )

    val dating = PregnancyDating(
        method = method,
        lmp = lmp.takeIf { method == DatingMethod.FUM && it.isNotBlank() },
        dueDate = dueDate.takeIf { method == DatingMethod.FUM && it.isNotBlank() },
        referenceDate = referenceDate.takeIf { method != DatingMethod.FUM && it.isNotBlank() },
        referenceWeeks = referenceWeeks.toIntOrNull().takeIf { method != DatingMethod.FUM },
        referenceDays = referenceDays.toIntOrNull().takeIf { method != DatingMethod.FUM },
    )
    val weeks = referenceWeeks.toIntOrNull()
    val days = referenceDays.toIntOrNull()
    val validReference = method == DatingMethod.FUM ||
        (referenceDate.isNotBlank() && referenceDate <= todayIsoDate() && weeks != null && weeks in 0..42 && days != null && days in 0..6)
    val age = if (validReference) viewModel.preview(dating, todayIsoDate()) else null

    val datingToSave = profileDatingToSave(dating, initialProfile?.dating, validReference && age != null)

    fun next() {
        error = when (step) {
            1 -> if (country.isBlank()) Res.string.select_your_country else null
            2 -> if (datingToSave != null) null else when {
                method == DatingMethod.FUM && lmp.isBlank() && dueDate.isBlank() -> Res.string.enter_your_last_menstrual_period_or_estimated_due
                !validReference -> Res.string.enter_a_date_and_a_valid_gestational_age
                age == null -> Res.string.the_dates_do_not_correspond_to_an_ongoing
                else -> null
            }
            else -> null
        }
        if (error == null) step++
    }

    if (step == 0) {
        WelcomeScreen(onStart = { step = 1 })
        return
    }

    Column(
        Modifier.fillMaxSize().screenEntrance().then(if (initialProfile == null) Modifier.safeDrawingPadding() else Modifier)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(ui.s(Res.string.brand), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = ::back, enabled = !saving) { Text(ui.s(Res.string.back)) }
            Column(Modifier.weight(1f)) {
                Text(ui.s(Res.string.your_setup_of_3, step), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(step / 3f, 0f..1f)
                }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        Box(Modifier.weight(1f).height(6.dp).background(
                            if (index < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(50)))
                    }
                }
            }
        }
        when (step) {
            1 -> {
                EditorialHeader(ui.s(Res.string.tell_us_a_little_about_yourself), ui.s(Res.string.names_are_optional_you_can_skip_them))
                PastelTextField(motherName, { motherName = it }, label = { Text(ui.s(Res.string.your_name_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                PastelTextField(babyName, { babyName = it }, label = { Text(ui.s(Res.string.baby_s_nickname_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                CountryField(country) { country = it }
                Text(ui.s(Res.string.the_app_follows_your_device_language), style = MaterialTheme.typography.bodyMedium)
                Text(ui.s(Res.string.available_in_spanish_english_and_french), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            2 -> {
                EditorialHeader(ui.s(Res.string.pregnancy_dates))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        DatingMethod.FUM to ui.s(Res.string.last_menstrual_period),
                        DatingMethod.ULTRASOUND to ui.s(Res.string.ultrasound),
                        DatingMethod.MEDICAL to ui.s(Res.string.medical_adjustment),
                    ).forEach { (value, label) ->
                        FilterChip(selected = method == value, onClick = { method = value; error = null }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp))
                    }
                }
                if (method == DatingMethod.FUM) {
                    DateField(ui.s(Res.string.first_day_of_your_last_menstrual_period), lmp, maxDate = todayIsoDate(), clearable = true) { lmp = it }
                    DateField(ui.s(Res.string.estimated_due_date_if_known), dueDate, clearable = true) { dueDate = it }
                } else {
                    DateField(ui.s(Res.string.date_established), referenceDate, maxDate = todayIsoDate()) { referenceDate = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PastelTextField(referenceWeeks, { referenceWeeks = it }, label = { Text(ui.s(Res.string.weeks)) }, modifier = Modifier.weight(1f), singleLine = true)
                        PastelTextField(referenceDays, { referenceDays = it }, label = { Text(ui.s(Res.string.days)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                if (age != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(ui.s(Res.string.today_wk_d, age.weeks, age.days), fontWeight = FontWeight.Bold)
                            Text(ui.s(Res.string.estimated_due_date_2, dateLabel(ui, age.dueDate)))
                        }
                    }
                }
                if (age == null && datingToSave != null && initialProfile != null) Text(ui.s(Res.string.your_saved_dates_will_be_preserved_you_can), style = MaterialTheme.typography.bodySmall)
                Text(ui.s(Res.string.calculations_are_estimates_ultrasound_or_medical_dating_takes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            3 -> {
                EditorialHeader(ui.s(Res.string.your_information_belongs_to_you))
                EditorialRecordCard {
                        Text(ui.s(Res.string.this_profile_will_be_saved_securely_on_this))
                        Text(ui.s(Res.string.gestiora_does_not_diagnose_or_replace_your_care))
                        Text(ui.s(Res.string.data_recovery_on_another_device_is_not_available))
                }
            }
        }
        error?.let { Text(ui.s(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (saveFailed) Text(ui.s(Res.string.we_couldn_t_save_your_profile_try_again), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        GradientButton(
            onClick = {
                if (step < 3) next()
                else {
                    // The confirmation step can remain open across a calendar-day change.
                    val submitToday = todayIsoDate()
                    val submittedDating = profileDatingToSave(dating, initialProfile?.dating,
                        validReference && (method == DatingMethod.FUM || referenceDate <= submitToday) &&
                            viewModel.preview(dating, submitToday) != null)
                    if (submittedDating == null) { step = 2; error = Res.string.check_the_pregnancy_dates_before_saving }
                    else onSave(PregnancyProfile(motherName.trim().ifEmpty { null }, babyName.trim().ifEmpty { null }, country, initialProfile?.language ?: deviceLanguage, submittedDating))
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) ui.s(Res.string.saving) else if (step < 3) ui.s(Res.string.action_continue) else ui.s(Res.string.start_using_gestiora)) }
    }
}
@Composable
private fun CountryField(value: String, onValueChange: (String) -> Unit) {
    val ui = LocalUiCopy.current
    var expanded by retain { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(if (value.isEmpty()) ui.s(Res.string.country_select) else ui.s(Res.string.country_2, countryLabel(value, ui))) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            countries.forEach { country -> DropdownMenuItem(text = { Text(countryLabel(country, ui)) }, onClick = { onValueChange(country); expanded = false }) }
        }
    }
}
