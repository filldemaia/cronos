package com.filldemaia.cronos.radio.ui.components

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.filldemaia.cronos.CronosWidget
import com.filldemaia.cronos.CronosWidgetApple
import com.filldemaia.cronos.CronosWidgetLarge
import com.filldemaia.cronos.R
import com.filldemaia.cronos.SettingsRepository
import com.filldemaia.cronos.WidgetType
import com.filldemaia.cronos.WidgetUpdateScheduler
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioText
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsCard(settings: SettingsRepository) {
    val context = LocalContext.current
    var sizes by remember { mutableStateOf(WidgetType.entries.associateWith(settings::widgetTextSize)) }
    var previewOpen by remember { mutableStateOf(false) }

    RadioCard {
        SectionHeader(title = "Widgets")
        Text("Mida del text", style = MaterialTheme.typography.bodySmall, color = RadioMuted)
        WidgetType.entries.forEach { type ->
            val name = stringResource(type.nameResource())
            Row(Modifier.fillMaxWidth()) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = RadioText)
                Text("${sizes.getValue(type).roundToInt()}", color = RadioOrange)
            }
            CronosSlider(
                value = sizes.getValue(type),
                onValueChange = { sizes = sizes + (type to it) },
                onValueChangeFinished = {
                    settings.setWidgetTextSize(type, sizes.getValue(type))
                    WidgetUpdateScheduler.updateAll(context)
                },
                valueRange = WidgetType.MIN_TEXT_SIZE_SP..WidgetType.MAX_TEXT_SIZE_SP,
                steps = 27,
                modifier = Modifier.semantics { contentDescription = "Mida del text de $name" }
            )
        }
        Text(
            "Si no hi cap, el text s'ajusta al widget.",
            style = MaterialTheme.typography.bodySmall,
            color = RadioMuted
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { previewOpen = true }) { Text("Previsualitza") }
            TextButton(onClick = {
                settings.resetWidgets()
                sizes = WidgetType.entries.associateWith(settings::widgetTextSize)
                WidgetUpdateScheduler.updateAll(context)
            }) { Text("Restableix") }
        }
    }

    if (previewOpen) {
        AlertDialog(
            onDismissRequest = { previewOpen = false },
            title = { Text("Widgets") },
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WidgetType.entries.forEach { type ->
                        Text(stringResource(type.nameResource()), color = RadioMuted)
                        WidgetPreview(type)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { previewOpen = false }) { Text("Tanca") } }
        )
    }
}

private fun WidgetType.nameResource(): Int = when (this) {
    WidgetType.DATE_AND_TIME -> R.string.widget_name_large
    WidgetType.TIME -> R.string.widget_name
    WidgetType.SENYERA -> R.string.widget_name_apple
}

@Composable
private fun WidgetPreview(type: WidgetType) {
    val provider = remember(type) {
        when (type) {
            WidgetType.DATE_AND_TIME -> CronosWidgetLarge()
            WidgetType.TIME -> CronosWidget()
            WidgetType.SENYERA -> CronosWidgetApple()
        }
    }
    val height = when (type) {
        WidgetType.DATE_AND_TIME -> 150.dp
        WidgetType.TIME -> 76.dp
        WidgetType.SENYERA -> 164.dp
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
        val widthDp = maxWidth.value
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(height),
            factory = { FrameLayout(it) },
            update = { host ->
                host.removeAllViews()
                val views = provider.createViews(host.context, widthDp, height.value)
                host.addView(views.apply(host.context, host), FrameLayout.LayoutParams(-1, -1))
            }
        )
    }
}
