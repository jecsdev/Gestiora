package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gestiora.shared.presentation.ui.generated.resources.*

/** Keep the list and its scroll position behind the editor. Hidden is vetoed until the
 * screen's existing discard confirmation removes the draft; Keep editing never hides it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordFormSheet(busy: Boolean, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val ui = LocalUiCopy.current
    val currentBusy by rememberUpdatedState(busy)
    val requestDismiss by rememberUpdatedState(onDismissRequest)
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val state = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { value ->
            if (value == SheetValue.Hidden) {
                if (!currentBusy) requestDismiss()
                false
            } else true
        },
    )
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismissRequest() },
        sheetState = state,
        modifier = Modifier.fillMaxWidth(),
        sheetGesturesEnabled = !busy,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !busy, shouldDismissOnClickOutside = !busy),
        dragHandle = {
            Box(Modifier.fillMaxWidth().height(48.dp)) {
                BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.Center))
                IconButton(onClick = onDismissRequest, enabled = !busy,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                        .semantics { contentDescription = ui.s(Res.string.cancel) }) {
                    Text("×", fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics {})
                }
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().heightIn(max = (windowHeight * 0.88f - 48.dp).coerceAtLeast(160.dp))) {
            content()
        }
    }
}
