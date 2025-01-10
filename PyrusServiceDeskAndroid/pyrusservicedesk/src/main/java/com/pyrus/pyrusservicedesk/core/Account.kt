package com.pyrus.pyrusservicedesk.core

import android.os.Parcelable
import com.pyrus.pyrusservicedesk.User
import kotlinx.parcelize.Parcelize

internal sealed interface Account: Parcelable {

    val domain: String

    @Parcelize
    data class V1(
        override val domain: String,
        val userId: String,
        val appId: String,
    ) : Account

    @Parcelize
    data class V2(
        override val domain: String,
        val instanceId: String,
        val appId: String,
        val userId: String,
        val securityKey: String,
    ): Account

    @Parcelize
    data class V3(
        override val domain: String,
        val instanceId: String,
        val firstAppId: String,
        val firstUserId: String,
        val users: List<User>, //users have userId, userName, authorId
        val authorId: String,
    ): Account



}