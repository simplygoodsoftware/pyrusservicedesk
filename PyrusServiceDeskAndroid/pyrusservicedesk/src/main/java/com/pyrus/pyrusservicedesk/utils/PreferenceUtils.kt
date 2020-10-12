package com.pyrus.pyrusservicedesk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

internal const val PREFERENCE_KEY = "com.pyrus.pyrusservicedesk.PREFERENCES"
private const val PREFERENCE_KEY_LEGACY = "net.papirus.pyrusservicedesk.PREFERENCES"

internal const val PREFERENCE_KEY_USER_ID = "PREFERENCE_KEY_USER_ID"
private const val PREFERENCE_KEY_USER_ID_LEGACY = "net.papirus.pyrusservicedesk.PREFS_KEY_USER_ID"

internal const val PREFERENCE_KEY_DRAFT = "PREFERENCE_KEY_DRAFT"
private const val PREFERENCE_KEY_DRAFT_LEGACY = "net.papirus.pyrusservicedesk.PREFERENCE_KEY_DRAFT"

private const val PREFERENCE_KEY_VERSION = "PREFERENCE_KEY_VERSION"

internal const val PREFERENCE_KEY_LAST_SET_TOKEN = "PREFERENCE_KEY_LAST_SET_TOKEN"
internal const val PREFERENCE_KEY_LAST_ACTIVITY_TIME = "PREFERENCE_KEY_LAST_ACTIVITY_TIME"

/**
 * Current version to migrate user preferences to
 */
private const val MIGRATE_TO_VERSION = 1

/**
 * Updates current preferences to the actual version by applying incremental migration
 * @param context context
 * @param preferences to apply migration to if need
 */
internal fun migratePreferences(context: Context, preferences: SharedPreferences) {
    if (!isMigrationRequired(preferences)) {
        return
    }

    val updateVersion = preferences.getInt(PREFERENCE_KEY_VERSION, 0)
    when (updateVersion) {
        0 -> toVersion1(context, preferences)
    }
    migratePreferences(context, preferences)
}

/**
 * Checks whether migration to a newer version is needed
 *
 * @param preferences actual preferences
 * @return true if preferences should be updated to a newer version
 */
private fun isMigrationRequired(preferences: SharedPreferences): Boolean {
    return MIGRATE_TO_VERSION > preferences.getInt(PREFERENCE_KEY_VERSION, 0)
}

@SuppressLint("ApplySharedPref")
private fun toVersion1(context: Context, preferences: SharedPreferences) {
    val legacy = context.getSharedPreferences(PREFERENCE_KEY_LEGACY, Context.MODE_PRIVATE)
    if (legacy.all?.isEmpty() == false) {
        preferences
            .edit()
            .putString(PREFERENCE_KEY_DRAFT, legacy.getString(PREFERENCE_KEY_DRAFT_LEGACY, ""))
            .commit()
        preferences
            .edit()
            .putString(PREFERENCE_KEY_USER_ID, legacy.getString(PREFERENCE_KEY_USER_ID_LEGACY, ""))
            .commit()
        legacy.edit().clear().apply()
    }
    preferences.edit().putInt(PREFERENCE_KEY_VERSION, 1).commit()
}