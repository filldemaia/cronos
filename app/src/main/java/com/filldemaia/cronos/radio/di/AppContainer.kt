package com.filldemaia.cronos.radio.di

import android.content.Context
import com.filldemaia.cronos.radio.alarm.AlarmScheduler
import com.filldemaia.cronos.radio.data.local.DeviceProtectedStore
import com.filldemaia.cronos.radio.data.repository.ReliabilityRepository
import com.filldemaia.cronos.radio.data.repository.ScheduleRepository
import com.filldemaia.cronos.radio.data.repository.SettingsRepository
import com.filldemaia.cronos.radio.data.repository.StationRepository
import com.filldemaia.cronos.radio.playback.NotificationHelper
import com.filldemaia.cronos.radio.playback.PlaybackStateStore
import com.filldemaia.cronos.radio.reliability.ReliabilityChecker

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val deviceProtectedStore = DeviceProtectedStore(appContext)
    val notificationHelper = NotificationHelper(appContext)
    val settingsRepository = SettingsRepository(appContext, deviceProtectedStore)
    val scheduleRepository = ScheduleRepository(appContext, deviceProtectedStore)
    val stationRepository = StationRepository(appContext, deviceProtectedStore)
    val reliabilityRepository = ReliabilityRepository(appContext)
    val playbackStateStore = PlaybackStateStore()

    val alarmScheduler = AlarmScheduler(
        context = appContext,
        scheduleRepository = scheduleRepository,
        stationRepository = stationRepository,
        settingsRepository = settingsRepository,
        deviceProtectedStore = deviceProtectedStore
    )

    val reliabilityChecker = ReliabilityChecker(
        context = appContext,
        scheduleRepository = scheduleRepository,
        stationRepository = stationRepository,
        reliabilityRepository = reliabilityRepository,
        alarmScheduler = alarmScheduler
    )

    suspend fun refreshReliabilityNotification() {
        val report = reliabilityChecker.check()
        notificationHelper.updateReliability(report, report.activeScheduleCount)
    }
}
