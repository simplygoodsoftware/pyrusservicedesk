package com.pyrus.pyrusservicedesk.sdk.updates

import android.annotation.SuppressLint
import android.content.SharedPreferences

@SuppressLint("ApplySharedPref")
internal class PreferencesManager(private val preferences: SharedPreferences): Preferences {

    override fun saveLastComment(comment: LastComment) {
        preferences.edit()
            .putInt(LAST_COMMENT_ID, comment.id)
            .putBoolean(LAST_COMMENT_IS_READ, comment.isRead)
            .putBoolean(LAST_COMMENT_IS_SHOWN, comment.isShown)
            .putString(LAST_COMMENT_TEXT, comment.text)
            .putString(LAST_COMMENT_ATTACHES, serializeAttaches(comment.attaches))
            .putInt(LAST_COMMENT_ATTACHES_COUNT, comment.attachesCount)
            .putLong(LAST_COMMENT_UTC_TIME, comment.utcTime)
            .commit()
    }

    override fun getLastComment(): LastComment? {
        val id = preferences.getInt(LAST_COMMENT_ID, NO_ID)
        if (id == NO_ID)
            return null
        return LastComment(
            id,
            preferences.getBoolean(LAST_COMMENT_IS_READ, false),
            preferences.getBoolean(LAST_COMMENT_IS_SHOWN, false),
            preferences.getString(LAST_COMMENT_TEXT, null),
            deserializeAttaches(preferences.getString(LAST_COMMENT_ATTACHES, null)),
            preferences.getInt(LAST_COMMENT_ATTACHES_COUNT, 0),
            preferences.getLong(LAST_COMMENT_UTC_TIME, -1)
        )
    }

    override fun removeLastComment() {
        preferences.edit().putInt(LAST_COMMENT_ID, NO_ID).commit()
    }

    override fun saveLastActiveTime(time: Long) {
        preferences.edit().putLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, time).commit()
    }

    override fun getLastActiveTime(): Long {
        return preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L)
    }

    private fun serializeAttaches(attaches: List<String>?): String? {
        if (attaches == null)
            return null
        val serializedAttachments = StringBuilder()
        attaches.forEachIndexed { index, attachmentName ->
            if (index != 0)
                serializedAttachments.append(SEPARATOR)
            serializedAttachments.append(attachmentName)
        }
        return serializedAttachments.toString()
    }

    private fun deserializeAttaches(string: String?): List<String>? {
        if (string == null)
            return null
        return string.split(SEPARATOR)
    }

    companion object {
        private const val NO_ID = -1
        private const val SEPARATOR = '⋮'

        private const val LAST_COMMENT_ID = "LAST_COMMENT_ID"
        private const val LAST_COMMENT_IS_READ = "LAST_COMMENT_IS_READ"
        private const val LAST_COMMENT_IS_SHOWN = "LAST_COMMENT_IS_SHOWN"
        private const val LAST_COMMENT_TEXT = "LAST_COMMENT_TEXT"
        private const val LAST_COMMENT_ATTACHES = "LAST_COMMENT_ATTACHES"
        private const val LAST_COMMENT_ATTACHES_COUNT = "LAST_COMMENT_ATTACHES_COUNT"
        private const val LAST_COMMENT_UTC_TIME = "LAST_COMMENT_UTC_TIME"

        private const val PREFERENCE_KEY_LAST_ACTIVITY_TIME = "PREFERENCE_KEY_LAST_ACTIVITY_TIME"
    }

}