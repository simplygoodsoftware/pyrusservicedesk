package net.papirus.pyrusservicedesk.repository.web_service

import net.papirus.pyrusservicedesk.repository.web_service.retrofit.RetrofitWebService

internal class WebServiceFactory {
    companion object {
        fun create(appId: String, userId: String) = RetrofitWebService(appId, userId)
    }
}
