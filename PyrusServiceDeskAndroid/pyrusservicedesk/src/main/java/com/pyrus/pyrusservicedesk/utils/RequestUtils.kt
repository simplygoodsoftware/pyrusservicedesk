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
        private const val PYRUS_BASE_URL = "https://dev.pyrus.com/servicedeskapi/v1/"

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
        internal fun getBaseLogoUrl(domain: String?): String {
            if (domain == null) {
                return "https://dev.pyrus.com"
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
        internal fun getFileUrl(fileId: Int, domain: String?): String {
            return "${getBaseUrl(domain)}DownloadFile/$fileId" + getPathParams()
        }

        internal fun getPreviewUrl(fileId: Int, domain: String?): String {
            return "${getBaseUrl(domain)}DownloadFilePreview/$fileId" + getPathParams()
        }

        //TODO ""
        private fun getPathParams(): String {
            val version = PyrusServiceDesk.get().apiVersion
            return with(PyrusServiceDesk.get()) {
                if (version == API_VERSION_2)
                    "?user_id=" +
                            URLEncoder.encode(userId, "UTF-8") +
                            "&security_key=" +
                            URLEncoder.encode(PyrusServiceDesk.get().securityKey, "UTF-8") +
                            "&instance_id=" +
                            URLEncoder.encode("", "UTF-8") +
                            "&version=" +
                            URLEncoder.encode(version.toString(), "UTF-8") +
                            "&app_id=" +
                            URLEncoder.encode(appId, "UTF-8")
                else
                    "?user_id=" +
                            URLEncoder.encode("", "UTF-8") +
                            "&app_id=" +
                            URLEncoder.encode(appId, "UTF-8")
            }
        }
    }
}

