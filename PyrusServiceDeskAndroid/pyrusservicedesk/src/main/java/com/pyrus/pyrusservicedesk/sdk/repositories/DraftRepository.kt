package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY_DRAFT

/**
 * DraftRepository implementation based on usage of [SharedPreferences] as the private storage.
 */
internal class DraftRepository(private val preferences: SharedPreferences) {

    /**
     * Provides stored draft.
     */
    fun getDraft(): String = preferences.getString(PREFERENCE_KEY_DRAFT, "")!!

    /**
     * Saves given [draft]
     */
    fun saveDraft(draft: String) {
        preferences.edit().putString(PREFERENCE_KEY_DRAFT, draft).apply()
    }
}