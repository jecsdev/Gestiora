package com.jecsdev.gestiora

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.presentation.ui.NativeAdContent

/** Local creative for checking layout in the internal flavor. */
internal val nativeAdContent: NativeAdContent = {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.sample_native_ad_label),
                color = colors.onSurfaceVariant,
                style = typography.labelSmall,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Onda Audio", style = typography.titleSmall)
                    Text(
                        stringResource(R.string.sample_native_ad_headline),
                        color = colors.onSurfaceVariant,
                        style = typography.bodySmall,
                    )
                }
                Surface(
                    modifier = Modifier.widthIn(max = 110.dp),
                    shape = MaterialTheme.shapes.small,
                    color = colors.surfaceVariant,
                    contentColor = colors.onSurfaceVariant,
                ) {
                    Text(
                        stringResource(R.string.sample_native_ad_action),
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = typography.labelMedium,
                    )
                }
            }
        }
    }
}
