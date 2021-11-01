package com.pyrus.pyrusservicedesk.utils

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import java.net.URLEncoder

@Keep
class RequestUtils{
    companion object {
        /**
         * Max supported file sizes.
         */
        const val MAX_FILE_SIZE_MEGABYTES = 250
        const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MEGABYTES * 1024 * 1024

        /**
         * Base URL used for api calls
         */
        internal const val BASE_URL = "https://pyrus.com/servicedeskapi/v1/"

        /**
         * Provides url for getting the avatar.
         */
        internal fun getAvatarUrl(avatarId: Int, domain: String?): String = "${getBaseUrl(domain)}/Avatar/$avatarId"

        /**
         * @return baseU
         */
        internal fun getBaseUrl(domain: String?): String {
            if (domain == null) {
                return "https://pyrus.com/servicedeskapi/v1/"
            }
            return "https://$domain/servicedeskapi/v1/"
        }

        /**
         * Provides url for getting the file.
         */
        internal fun getFileUrl(fileId: Int, domain: String?): String {
            return "${getBaseUrl(domain)}DownloadFile/$fileId" + getPathParams()
        }

        internal fun getPreviewUrl(fileId: Int, domain: String?): String {
            return "${getBaseUrl(domain)}DownloadFilePreview/$fileId" + getPathParams()
        }

        private fun getPathParams(): String {
            val version = PyrusServiceDesk.get().apiVersion
            return with(PyrusServiceDesk.get()) {
                if (version == API_VERSION_2)
                    "?user_id=" +
                            URLEncoder.encode(userId, "UTF-8") +
                            "&security_key=" +
                            URLEncoder.encode(PyrusServiceDesk.get().securityKey, "UTF-8") +
                            "&instance_id=" +
                            URLEncoder.encode(instanceId, "UTF-8") +
                            "&version=" +
                            URLEncoder.encode(version.toString(), "UTF-8") +
                            "&app_id=" +
                            URLEncoder.encode(appId, "UTF-8")
                else
                    "?user_id=" +
                            URLEncoder.encode(instanceId, "UTF-8") +
                            "&app_id=" +
                            URLEncoder.encode(appId, "UTF-8")
            }
        }
    }
}

