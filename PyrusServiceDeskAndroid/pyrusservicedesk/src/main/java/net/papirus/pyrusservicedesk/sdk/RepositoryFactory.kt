package net.papirus.pyrusservicedesk.sdk

import android.content.SharedPreferences
import net.papirus.pyrusservicedesk.sdk.repositories.draft.DraftRepository
import net.papirus.pyrusservicedesk.sdk.repositories.draft.PreferenceDraftRepository
import net.papirus.pyrusservicedesk.sdk.repositories.general.CentralRepository
import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.web.retrofit.RetrofitWebRepository

internal class RepositoryFactory(private val fileResolver: FileResolver,
                                 private val sharedPreferences: SharedPreferences) {

    fun createCentralRepository(appId: String, userId: String): GeneralRepository =
        CentralRepository(
            RetrofitWebRepository(
                appId,
                userId,
                fileResolver
            )
        )

    fun createDraftRepository(): DraftRepository {
        return PreferenceDraftRepository(sharedPreferences)
    }
}