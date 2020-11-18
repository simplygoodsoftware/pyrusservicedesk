package com.pyrus.pyrusservicedesk.utils

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
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
        internal fun getAvatarUrl(avatarId: Int): String = "$BASE_URL/Avatar/$avatarId"

        /**
         * Provides url for getting the file.
         */
        internal fun getFileUrl(fileId: Int): String {
            return "${BASE_URL}DownloadFile/$fileId" + getPathParams()
        }

        internal fun getPreviewUrl(fileId: Int): String {
            return "${BASE_URL}DownloadFilePreview/$fileId" + getPathParams()
        }

        private fun getPathParams(): String {
            val version = PyrusServiceDesk.get().apiVersion
            return with(PyrusServiceDesk.get()) {
                if (version == 1)
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

