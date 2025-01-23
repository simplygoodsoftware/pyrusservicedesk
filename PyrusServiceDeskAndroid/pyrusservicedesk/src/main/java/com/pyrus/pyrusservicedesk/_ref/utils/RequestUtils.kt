package com.pyrus.pyrusservicedesk._ref.utils

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk.core.Account
import java.net.URLEncoder

@Keep
internal object RequestUtils{
    /**
     * Max supported file sizes.
     */
    const val MAX_FILE_SIZE_MEGABYTES = 250
    const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MEGABYTES * 1024 * 1024

    /**
     * Base URL used for api calls
     */
    private const val PYRUS_BASE_URL = "https://pyrus.com/servicedeskapi/v1/"

    /**
     * Provides url for getting the avatar.
     */
    internal fun getAvatarUrl(avatarId: Int, domain: String?): String = "${getBaseUrl(domain)}Avatar/$avatarId"

    /**
     * Provides url for getting the organisation logo.
     */
    internal fun getOrganisationLogoUrl(orgLogoUrl: String, domain: String?): String = "${getBaseLogoUrl(domain)}$orgLogoUrl"

    /**
     * return vase url for org logo
     */
    private fun getBaseLogoUrl(domain: String?): String {
        if (domain == null) {
            return "https://pyrus.com"
        }
        return "https://$domain"
    }

    /**
     * @return baseU
     */
    internal fun getBaseUrl(domain: String?): String {
        if (domain == null) {
            return PYRUS_BASE_URL
        }
        return "https://$domain/servicedeskapi/v1/"
    }

    /**
     * Provides url for getting the file.
     */
    internal fun getFileUrl(fileId: Long, account: Account, multichatUser: User?): String {
        return "${getBaseUrl(account.domain)}DownloadFile/$fileId" + getPathParams(account, multichatUser)
    }

    internal fun getPreviewUrl(fileId: Long, account: Account, multichatUser: User?): String {
        return "${getBaseUrl(account.domain)}DownloadFilePreview/$fileId" + getPathParams(account, multichatUser)
    }

    private fun getPathParams(account: Account, multichatUser: User?): String = when(account) {
        is Account.V1 -> {
            "?user_id=" +
                URLEncoder.encode(account.userId, "UTF-8") +
                "&app_id=" +
                URLEncoder.encode(account.appId, "UTF-8")
        }
        is Account.V2 -> {
            "?user_id=" +
                URLEncoder.encode(account.userId, "UTF-8") +
                "&security_key=" +
                URLEncoder.encode(account.securityKey, "UTF-8") +
                "&instance_id=" +
                URLEncoder.encode(account.instanceId, "UTF-8") +
                "&version=" +
                URLEncoder.encode("2", "UTF-8") +
                "&app_id=" +
                URLEncoder.encode(account.appId, "UTF-8")
        }

        is Account.V3 -> {
            "?user_id=" +
                URLEncoder.encode(account.users.find { it == multichatUser }?.userId ?: "", "UTF-8") +
                "&app_id=" +
                URLEncoder.encode(account.users.find { it == multichatUser }?.appId ?: "", "UTF-8")
        }
    }
}

