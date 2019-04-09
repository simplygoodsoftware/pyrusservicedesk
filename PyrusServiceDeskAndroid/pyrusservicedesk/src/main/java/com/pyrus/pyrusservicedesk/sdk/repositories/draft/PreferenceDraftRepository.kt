package com.pyrus.pyrusservicedesk.sdk.repositories.draft

import android.content.SharedPreferences
import com.pyrus.pyrusservicedesk.utils.PREFERENCE_KEY_DRAFT

/**
 * [DraftRepository] implementation based on usage of [SharedPreferences] as the private storage.
 */
internal class PreferenceDraftRepository(private val preferences: SharedPreferences): DraftRepository {

    override fun getDraft(): String = preferences.getString(PREFERENCE_KEY_DRAFT, "")

    override fun saveDraft(draft: String) {
        preferences.edit().putString(PREFERENCE_KEY_DRAFT, draft).apply()
    }
}