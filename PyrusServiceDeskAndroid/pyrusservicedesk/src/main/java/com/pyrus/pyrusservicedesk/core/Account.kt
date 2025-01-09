package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.User

internal sealed interface Account {

    val domain: String

    data class V1(
        override val domain: String,
        val userId: String,
        val appId: String,
    ) : Account

    data class V2(
        override val domain: String,
        val instanceId: String,
        val appId: String,
        val userId: String,
        val securityKey: String,
    ): Account

    data class V3(
        override val domain: String,
        val instanceId: String,
        val firstAppId: String,
        val firstUserId: String,
        val users: List<User>, //users have userId, userName, authorId
        val authorId: String,
    ): Account



}