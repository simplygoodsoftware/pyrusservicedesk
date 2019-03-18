package net.papirus.pyrusservicedesk.sdk.web_service

import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.RetrofitWebService

internal class WebServiceFactory {
    companion object {
        fun create(appId: String, userId: String, isFeed: Boolean) = RetrofitWebService(appId, userId, isFeed)
    }
}
