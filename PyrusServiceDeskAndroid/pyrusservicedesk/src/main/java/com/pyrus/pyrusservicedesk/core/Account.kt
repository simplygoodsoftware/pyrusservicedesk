package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.User

internal sealed interface Account {

    val instanceId: String
    val appId: String
    val domain: String
    val isMultiChat: Boolean

    data class V1(
        override val instanceId: String,
        override val appId: String,
        override val domain: String,
        override val isMultiChat: Boolean,
    ) : Account

    data class V2(
        override val instanceId: String,
        override val appId: String,
        override val domain: String,
        override val isMultiChat: Boolean,
        val userId: String,
        val securityKey: String,
    ): Account

    data class V3(
        override val instanceId: String,
        override val appId: String,
        override val domain: String,
        override val isMultiChat: Boolean,
        val firstUserId: String,
        val users: List<User>, //users have userId, userName, authorId
        val authorId: String,
    ): Account



}