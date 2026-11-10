package com.filldemaia.cronos.radio.playback

object PlaybackCommands {
    const val ACTION_START_SCHEDULE = "com.filldemaia.cronos.radio.action.START_SCHEDULE"
    const val ACTION_PLAY_STATION = "com.filldemaia.cronos.radio.action.PLAY_STATION"
    const val ACTION_TOGGLE_PLAY_PAUSE = "com.filldemaia.cronos.radio.action.TOGGLE_PLAY_PAUSE"
    const val ACTION_PLAY = "com.filldemaia.cronos.radio.action.PLAY"
    const val ACTION_PAUSE = "com.filldemaia.cronos.radio.action.PAUSE"
    const val ACTION_SEEK_RELATIVE = "com.filldemaia.cronos.radio.action.SEEK_RELATIVE"
    const val ACTION_SEEK_TO = "com.filldemaia.cronos.radio.action.SEEK_TO"
    const val ACTION_GO_LIVE = "com.filldemaia.cronos.radio.action.GO_LIVE"
    const val ACTION_STOP = "com.filldemaia.cronos.radio.action.STOP"
    const val ACTION_STOP_LOCAL_ALARM = "com.filldemaia.cronos.radio.action.STOP_LOCAL_ALARM"
    const val ACTION_SLEEP_TIMER = "com.filldemaia.cronos.radio.action.SLEEP_TIMER"

    const val EXTRA_STATION_ID = "extra_station_id"
    const val EXTRA_SCHEDULE_JSON = "extra_schedule_json"
    const val EXTRA_PLANNED_AT = "extra_planned_at"
    const val EXTRA_IS_TEST = "extra_is_test"
    const val EXTRA_VOLUME = "extra_volume"
    const val EXTRA_FADE_SECONDS = "extra_fade_seconds"
    const val EXTRA_STOP_AFTER_MINUTES = "extra_stop_after_minutes"
    const val EXTRA_FALLBACK_IDS = "extra_fallback_ids"
    const val EXTRA_SEEK_US = "extra_seek_us"
    const val EXTRA_SLEEP_MINUTES = "extra_sleep_minutes"

    const val NOTIFICATION_ID = 100
}
