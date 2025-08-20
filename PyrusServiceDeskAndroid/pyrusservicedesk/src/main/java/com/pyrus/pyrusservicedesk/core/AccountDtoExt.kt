package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk.core.StaticRepository.getConfiguration
import com.pyrus.pyrusservicedesk.sdk.data.UserDataDto
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.sync.SyncMapper.calcLastNoteId

private const val API_VERSION_1: Int = 0
private const val API_VERSION_2: Int = 2
private const val API_VERSION_3: Int = 3

internal fun Account.getAuthorId(): String? = when(this) {
    is Account.V1 -> null
    is Account.V2 -> null
    is Account.V3 -> authorId
}

internal fun Account.getAppId(): String? = when(this) {
    is Account.V1 -> appId
    is Account.V2 -> appId
    is Account.V3 -> users.firstOrNull()?.appId
}

internal fun Account.getUserId(): String? = when(this) {
    is Account.V1 -> instanceId
    is Account.V2 -> userId
    is Account.V3 -> users.firstOrNull()?.userId
}

internal fun Account.getSecurityKey(): String? = when(this) {
    is Account.V1 -> null
    is Account.V2 -> securityKey
    is Account.V3 -> null
}

internal fun Account.getInstanceId(): String = when(this) {
    is Account.V1 -> instanceId
    is Account.V2 -> instanceId
    is Account.V3 -> instanceId
}

internal fun Account.getVersion(): Int = when(this) {
    is Account.V1 -> API_VERSION_1
    is Account.V2 -> API_VERSION_2
    is Account.V3 -> API_VERSION_3
}

internal fun Account.isMultiChat() = this is Account.V3

internal fun Account.getAdditionalUsers(tickets: List<TicketEntity>): List<UserDataDto>? = when(this) {
    is Account.V1 -> null
    is Account.V2 -> null
    is Account.V3 -> users.mapNotNull { user ->
        if (getUserId() == user.userId) null
        else mapToUserDataDto(tickets, user)
    }.ifEmpty { null }
}

internal fun Account.getUsers() : List<User> = when(this) {
    is Account.V1 -> listOf(User(instanceId, appId, getConfiguration().userName ?: ""))
    is Account.V2 -> listOf(User(userId, appId, getConfiguration().userName ?: ""))
    is Account.V3 -> users
}

internal fun Account.getExtraUsers(): List<User> = when (this) {
    is Account.V1 -> emptyList()
    is Account.V2 -> emptyList()
    is Account.V3 -> extraUsers
}

private fun mapToUserDataDto(tickets: List<TicketEntity>, user: User): UserDataDto = UserDataDto(
    appId = user.appId,
    userId = user.userId,
    securityKey = null,
    lastNoteId = user.userId?.let { calcLastNoteId(tickets, it) }
)