package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * DraftRepository implementation based on usage of [SharedPreferences] as the private storage.
 */
internal class DraftRepository(
    private val preferences: SharedPreferences,
    private val idStore: IdStore,
    moshi: Moshi,
) {

    private val jsonAdapter = moshi.adapter<List<DraftEntity>>(
        object : TypeToken<List<DraftEntity>>(){}.type
    )

    private val draftsStateFlow: MutableStateFlow<List<DraftEntity>> = MutableStateFlow(readDrafts())

    /**
     * Provides stored draft.
     */
    fun getDraft(ticketId: Long): String {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val localTicketId = idStore.getTicketLocalId(ticketId) ?: ticketId

        return draftsStateFlow.value.find { entity ->
            entity.ticketId == serverTicketId || entity.ticketId == localTicketId
        }?.text ?: ""
    }

    /**
     * Saves given [draft]
     */
    fun saveDraft(ticketId: Long, draft: String) {
        val entity = DraftEntity(ticketId, draft)

        var drafts = draftsStateFlow.value.toMutableList()
        drafts.let { list ->
            val existingIndex = list.indexOfFirst { it.ticketId == entity.ticketId }
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
            }
            list.add(entity)
        }
        if (drafts.size > MAX_DRAFTS) {
            drafts = drafts.subList(drafts.size - MAX_DRAFTS, drafts.size)
        }
        writeDrafts(drafts)
    }

    private fun readDrafts(): List<DraftEntity> {
        val rawJson = preferences.getString(PREFERENCE_KEY_DRAFTS, "[]")!!
        val draftList = (jsonAdapter.fromJson(rawJson)?: emptyList()).toMutableList()
        draftList.removeAll { it.text.isBlank() }
        return draftList
    }

    private fun writeDrafts(drafts: List<DraftEntity>) {
        val rawJson = jsonAdapter.toJson(drafts)
        preferences.edit().putString(PREFERENCE_KEY_DRAFTS, rawJson).apply()
        draftsStateFlow.value = drafts
    }

    companion object {
        private const val PREFERENCE_KEY_DRAFTS = "PREFERENCE_KEY_DRAFTS"
        private const val MAX_DRAFTS = 100
    }

}