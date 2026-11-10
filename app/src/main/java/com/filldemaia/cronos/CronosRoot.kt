package com.filldemaia.cronos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filldemaia.cronos.radio.playback.PlaybackMode
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.components.MiniPlayer
import com.filldemaia.cronos.radio.ui.screens.FullPlayerScreen
import com.filldemaia.cronos.radio.ui.screens.OnboardingScreen
import com.filldemaia.cronos.radio.ui.screens.PlayerScreen
import com.filldemaia.cronos.radio.ui.screens.ReliabilityScreen
import com.filldemaia.cronos.radio.ui.screens.SchedulesScreen
import com.filldemaia.cronos.radio.ui.screens.SettingsScreen
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioSurface

private enum class MainTab(val label: String, val icon: ImageVector) {
    RELLOTGE("Rellotge", Icons.Filled.Schedule),
    RADIO("Ràdio", Icons.Filled.Radio),
    ALARMES("Alarmes", Icons.Filled.Alarm),
    AJUSTAMENTS("Configuració", Icons.Filled.Settings)
}

@Composable
fun CronosRoot(viewModel: RadioViewModel, clockSettings: SettingsRepository) {
    val onboarded by viewModel.onboarded.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var fullPlayerOpen by rememberSaveable { mutableStateOf(false) }
    var reliabilityOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    when (onboarded) {
        null -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RadioOrange)
        }
        false -> OnboardingScreen(
            viewModel = viewModel,
            onFinish = {
                selectedTab = 0
                viewModel.completeOnboarding()
            }
        )
        true -> {
            if (reliabilityOpen) {
                BackHandler { reliabilityOpen = false }
                ReliabilityScreen(
                    viewModel = viewModel,
                    onOpenSchedules = {
                        reliabilityOpen = false
                        selectedTab = 2
                    },
                    onBack = { reliabilityOpen = false }
                )
            } else if (fullPlayerOpen) {
                FullPlayerScreen(
                    playback = playback,
                    settings = settings,
                    onClose = { fullPlayerOpen = false },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onStop = viewModel::stop,
                    onStopLocalAlarm = viewModel::stopLocalAlarm,
                    onSeekRelative = viewModel::seekRelative,
                    onGoLive = viewModel::goLive,
                    onSeekToUs = viewModel::seekToUs,
                    onSetSleepTimer = viewModel::setSleepTimer
                )
            } else {
                MainScaffold(
                    viewModel = viewModel,
                    clockSettings = clockSettings,
                    snackbarHostState = snackbarHostState,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onOpenPlayer = { fullPlayerOpen = true },
                    onOpenReliability = { reliabilityOpen = true }
                )
            }
        }
    }
}

@Composable
private fun MainScaffold(
    viewModel: RadioViewModel,
    clockSettings: SettingsRepository,
    snackbarHostState: SnackbarHostState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenReliability: () -> Unit
) {
    val tabs = MainTab.entries
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val miniPlayerVisible = playback.mode != PlaybackMode.IDLE || playback.station != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = miniPlayerVisible) {
                    MiniPlayer(
                        playback = playback,
                        onOpen = onOpenPlayer,
                        onTogglePlayPause = viewModel::togglePlayPause
                    )
                }
                NavigationBar(containerColor = RadioSurface) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { onSelectTab(index) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RadioOrange,
                                selectedTextColor = RadioOrange,
                                indicatorColor = RadioOrange.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tabs[selectedTab]) {
                MainTab.RELLOTGE -> ClockScreen(settings = clockSettings)
                MainTab.RADIO -> PlayerScreen(
                    viewModel = viewModel,
                    onOpenPlayer = onOpenPlayer
                )
                MainTab.ALARMES -> SchedulesScreen(viewModel = viewModel)
                MainTab.AJUSTAMENTS -> SettingsScreen(
                    viewModel = viewModel,
                    clockSettings = clockSettings,
                    onOpenReliability = onOpenReliability
                )
            }
        }
    }
}
