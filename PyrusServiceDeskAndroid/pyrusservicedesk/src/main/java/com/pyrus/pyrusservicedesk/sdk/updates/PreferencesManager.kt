package com.pyrus.pyrusservicedesk.sdk.updates

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.Exception
import java.lang.reflect.Type

@SuppressLint("ApplySharedPref")
internal class PreferencesManager(private val preferences: SharedPreferences): Preferences {

    private val gson = GsonBuilder().create()

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

    override fun setTokenRegisterTimeList(timeList: List<Long>) {
        val json = try {
            gson.toJson(timeList, timeListType)
        }
        catch (e: Exception) {
            return
        }
        preferences.edit().putString(PREFERENCE_KEY_TOKEN_TIME_LIST, json).commit()
    }

    override fun getTokenRegisterTimeList(): List<Long> {
        val timeListJson =
            preferences.getString(PREFERENCE_KEY_TOKEN_TIME_LIST, null) ?: return emptyList()
        return try {
            gson.fromJson(timeListJson, timeListType)
        }
        catch (e: Exception) {
            emptyList()
        }
    }

    override fun setLastTokenRegisterMap(timeMap: Map<String, Long>) {
        val json = try {
            gson.toJson(timeMap, timeMapType)
        }
        catch (e: Exception) {
            return
        }
        preferences.edit().putString(PREFERENCE_KEY_TOKEN_TIME_MAP, json).commit()
    }

    override fun getLastTokenRegisterMap(): Map<String, Long> {
        val timeMapJson =
            preferences.getString(PREFERENCE_KEY_TOKEN_TIME_MAP, null) ?: return emptyMap()
        return try {
            gson.fromJson(timeMapJson, timeMapType)
        }
        catch (e: Exception) {
            emptyMap()
        }
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
        const val S_NO_ID = "NO_ID"
        private const val SEPARATOR = '⋮'

        private const val LAST_COMMENT_ID = "LAST_COMMENT_ID"
        private const val LAST_COMMENT_IS_READ = "LAST_COMMENT_IS_READ"
        private const val LAST_COMMENT_IS_SHOWN = "LAST_COMMENT_IS_SHOWN"
        private const val LAST_COMMENT_TEXT = "LAST_COMMENT_TEXT"
        private const val LAST_COMMENT_ATTACHES = "LAST_COMMENT_ATTACHES"
        private const val LAST_COMMENT_ATTACHES_COUNT = "LAST_COMMENT_ATTACHES_COUNT"
        private const val LAST_COMMENT_UTC_TIME = "LAST_COMMENT_UTC_TIME"

        private const val PREFERENCE_KEY_LAST_ACTIVITY_TIME = "PREFERENCE_KEY_LAST_ACTIVITY_TIME"

        private const val PREFERENCE_KEY_TOKEN_TIME_MAP = "PREFERENCE_KEY_TOKEN_TIME_MAP"
        private const val PREFERENCE_KEY_TOKEN_TIME_LIST = "PREFERENCE_KEY_TOKEN_TIME_LIST"

        private val timeMapType: Type = object : TypeToken<Map<String, Long>>(){}.type
        private val timeListType: Type = object : TypeToken<List<Long>>(){}.type
    }

}