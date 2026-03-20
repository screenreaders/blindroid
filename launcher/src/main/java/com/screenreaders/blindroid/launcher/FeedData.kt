package com.screenreaders.blindroid.launcher

data class FeedData(
    val time: String,
    val date: String,
    val battery: String,
    val notifications: List<String>,
    val externalMode: Boolean,
    val externalAvailable: Boolean,
    val showAlarm: Boolean,
    val alarmText: String?,
    val showCalendar: Boolean,
    val calendarText: String?,
    val calendarPermissionGranted: Boolean,
    val showWeather: Boolean,
    val weatherText: String?,
    val showReminders: Boolean,
    val reminderText: String?,
    val showHeadphones: Boolean,
    val headphonesText: String?,
    val showNetwork: Boolean,
    val networkText: String?
)
