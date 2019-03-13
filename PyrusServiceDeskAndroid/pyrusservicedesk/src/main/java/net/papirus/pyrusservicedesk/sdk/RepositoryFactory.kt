package net.papirus.pyrusservicedesk.sdk

import android.content.ContentResolver
import net.papirus.pyrusservicedesk.sdk.data.LocalDataProvider
import net.papirus.pyrusservicedesk.sdk.web_service.WebService

internal class RepositoryFactory {
    companion object {
        fun create(webService: WebService,
                   contentResolver: ContentResolver,
                   localDataProvider: LocalDataProvider)
                : Repository {

            return RepositoryImpl(webService, contentResolver, localDataProvider)
        }
    }
}