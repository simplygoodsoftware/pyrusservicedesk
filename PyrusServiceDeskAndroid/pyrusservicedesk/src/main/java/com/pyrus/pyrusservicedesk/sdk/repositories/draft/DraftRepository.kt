package com.pyrus.pyrusservicedesk.sdk.repositories.draft

/**
 * Interface for implementing an object that is responsible for handling drafts.
 */
internal interface DraftRepository {
    /**
     * Provides stored draft.
     */
    fun getDraft(): String

    /**
     * Saves given [draft]
     */
    fun saveDraft(draft: String)
}