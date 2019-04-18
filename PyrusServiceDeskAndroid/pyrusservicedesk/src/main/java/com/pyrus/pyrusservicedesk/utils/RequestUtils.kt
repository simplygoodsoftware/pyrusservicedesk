package com.pyrus.pyrusservicedesk.utils

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import java.net.URLEncoder

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
            return with(PyrusServiceDesk.getInstance()){
                "$BASE_URL/DownloadFile/$fileId" +
                        "?user_id=" +
                        URLEncoder.encode(userId, "UTF-8") +
                        "&app_id=" +
                        URLEncoder.encode(appId, "UTF-8")
            }
        }

        internal fun getPreviewUrl(fileId: Int): String {
            return with(PyrusServiceDesk.getInstance()){
                "$BASE_URL/DownloadFilePreview/$fileId" +
                        "?user_id=" +
                        URLEncoder.encode(userId, "UTF-8") +
                        "&app_id=" +
                        URLEncoder.encode(appId, "UTF-8")
            }
        }
    }
}

