package net.papirus.pyrusservicedesk.sdk.repositories.draft

internal interface DraftRepository {
    fun getDraft(): String
    fun saveDraft(draft: String)
}