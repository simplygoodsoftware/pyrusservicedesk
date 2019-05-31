package com.pyrus.pyrusservicedesk.sdk.verify

import android.net.Uri

/**
 * Checks validness of local files
 */
internal interface LocalFileVerifier {
    /**
     * Checks whether file for [localFileUri] exists
     */
    fun isLocalFileExists(localFileUri: Uri?): Boolean
}