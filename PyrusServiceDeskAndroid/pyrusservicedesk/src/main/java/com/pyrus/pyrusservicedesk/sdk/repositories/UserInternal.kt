package com.pyrus.pyrusservicedesk.sdk.repositories

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class UserInternal(
    val userId: String,
    val appId: String,
) : Parcelable