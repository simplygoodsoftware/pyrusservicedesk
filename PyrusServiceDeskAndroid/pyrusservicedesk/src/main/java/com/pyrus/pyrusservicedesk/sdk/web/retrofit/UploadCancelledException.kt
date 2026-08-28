package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import java.io.IOException

/**
 * Signals that file uploading was stopped by [com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook.cancelUploading]
 * and must not be retried.
 */
internal class UploadCancelledException : IOException("Upload file canceled")
