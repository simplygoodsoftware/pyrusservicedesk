package com.pyrus.pyrusservicedesk.sdk.updates

import android.annotation.SuppressLint
import android.content.SharedPreferences

@SuppressLint("ApplySharedPref")
internal class PreferencesManager(private val preferences: SharedPreferences): Preferences {

    override fun saveLastActiveTime(time: Long) {
        preferences.edit().putLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, time).commit()
    }

    override fun getLastActiveTime(): Long {
        return preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L)
    }

    companion object {
        private const val PREFERENCE_KEY_LAST_ACTIVITY_TIME = "PREFERENCE_KEY_LAST_ACTIVITY_TIME"
    }

}