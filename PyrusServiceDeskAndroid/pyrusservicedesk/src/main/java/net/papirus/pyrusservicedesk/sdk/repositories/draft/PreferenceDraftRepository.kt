package net.papirus.pyrusservicedesk.sdk.repositories.draft

import android.content.SharedPreferences

private const val PREFERENCE_KEY_DRAFT = "net.papirus.pyrusservicedesk.PREFERENCE_KEY_DRAFT"

internal class PreferenceDraftRepository(private val preferences: SharedPreferences): DraftRepository {

    override fun getDraft(): String = preferences.getString(PREFERENCE_KEY_DRAFT, "")

    override fun saveDraft(draft: String) {
        preferences.edit().putString(PREFERENCE_KEY_DRAFT, draft).apply()
    }
}