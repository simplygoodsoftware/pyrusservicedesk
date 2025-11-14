package com.pyrus.pyrusservicedesk.sdk.repositories

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserInternal(
    val userId: String,
    val appId: String,
) : Parcelable