package net.papirus.pyrusservicedesk.repository

import android.content.ContentResolver
import net.papirus.pyrusservicedesk.repository.web_service.WebService

internal class RepositoryFactory {
    companion object {
        fun create(webService: WebService,
                   contentResolver: ContentResolver): Repository {

            return RepositoryImpl(webService, contentResolver)
        }
    }
}