package com.example.cronos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Pantalla de configuració. Sense bombolles — línies clares
 * sobre el fons del sistema, amb un scroll vertical per si
 * creix amb el temps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
) {
    var showSeconds by remember(settings) { mutableStateOf(settings.showSeconds) }
    var showSecondsWritten by remember(settings) { mutableStateOf(settings.showSecondsWritten) }
    var showDigital by remember(settings) { mutableStateOf(settings.showDigital) }
    var hourSize by remember(settings) { mutableStateOf(settings.hourSize) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_section_display))
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_digital),
                description = stringResource(R.string.settings_show_digital_desc),
                checked = showDigital,
                onCheckedChange = {
                    showDigital = it
                    settings.showDigital = it
                },
            )
            HorizontalDividerLegacy()
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_seconds),
                description = stringResource(R.string.settings_show_seconds_desc),
                checked = showSeconds && showDigital,
                onCheckedChange = {
                    showSeconds = it
                    settings.showSeconds = it
                },
                enabled = showDigital,
            )
            HorizontalDividerLegacy()
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_seconds_written),
                description = stringResource(R.string.settings_show_seconds_written_desc),
                checked = showSecondsWritten,
                onCheckedChange = {
                    showSecondsWritten = it
                    settings.showSecondsWritten = it
                },
            )
            HorizontalDividerLegacy()

            // Mida de l'hora catalana amb control lliscant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_hour_size),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${hourSize.roundToInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = hourSize,
                onValueChange = {
                    hourSize = it
                    settings.hourSize = it
                },
                valueRange = 18f..48f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp),
            )
            HorizontalDividerLegacy()

            SectionHeader(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = {
                    Text(stringResource(R.string.about_text))
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
            )

            HorizontalDividerLegacy()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(
                    onClick = {
                        settings.reset()
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.settings_reset))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp
        ),
    )
}

@Composable
private fun HorizontalDividerLegacy() {
    androidx.compose.material3.Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}
