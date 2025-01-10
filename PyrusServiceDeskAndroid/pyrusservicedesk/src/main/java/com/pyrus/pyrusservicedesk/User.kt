package com.pyrus.pyrusservicedesk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: String,
    val appId: String,
    val userName: String
) : Parcelable
