package com.pyrus.pyrusservicedesk.sdk

import android.content.SharedPreferences
import com.pyrus.pyrusservicedesk.sdk.repositories.draft.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.draft.PreferenceDraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.general.CentralRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RetrofitWebRepository

/**
 * Factory for making repository instances.
 */
internal class RepositoryFactory(private val fileResolver: FileResolver,
                                 private val sharedPreferences: SharedPreferences) {

    /**
     * Prepares main repository instances that handles base use cases of the app.
     */
    fun createCentralRepository(appId: String, userId: String): GeneralRepository =
        CentralRepository(
            RetrofitWebRepository(
                appId,
                userId,
                fileResolver
            )
        )

    /**
     * Creates repository tha handles drafts.
     */
    fun createDraftRepository(): DraftRepository {
        return PreferenceDraftRepository(sharedPreferences)
    }
}