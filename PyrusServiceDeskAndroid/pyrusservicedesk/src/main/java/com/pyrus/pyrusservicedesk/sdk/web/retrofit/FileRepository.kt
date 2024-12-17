package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.http.Url

internal class FileRepository(
    private val api: ServiceDeskApi
) {

    private val sendingAttachment = MutableStateFlow<List<Uri>>(emptyList())


    fun sendFile(uri: Uri) {

    }

    fun cancelSending(uri: Uri) {

    }


}