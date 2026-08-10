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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

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
    var compactExact by remember(settings) { mutableStateOf(settings.useCompactExactHour) }

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
                title = stringResource(R.string.settings_show_seconds),
                description = stringResource(R.string.settings_show_seconds_desc),
                checked = showSeconds,
                onCheckedChange = {
                    showSeconds = it
                    settings.showSeconds = it
                },
            )
            HorizontalDividerLegacy()

            SectionHeader(stringResource(R.string.settings_section_time))
            SettingSwitchRow(
                title = stringResource(R.string.settings_compact_exact),
                description = stringResource(R.string.settings_compact_exact_desc),
                checked = compactExact,
                onCheckedChange = {
                    compactExact = it
                    settings.useCompactExactHour = it
                },
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
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}
