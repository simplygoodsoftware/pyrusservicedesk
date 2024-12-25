package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk.sdk.data.UserDataDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.sync.SyncMapper.calcLastNoteId

private const val API_VERSION_1: Int = 0
private const val API_VERSION_2: Int = 2
private const val API_VERSION_3: Int = 3

internal fun Account.getAuthorId(): String? = when(this) {
    is Account.V1 -> null
    is Account.V2 -> null
    is Account.V3 -> authorId
}

internal fun Account.getUserId(): String = when(this) {
    is Account.V1 -> instanceId
    is Account.V2 -> userId
    is Account.V3 -> firstUserId
}

internal fun Account.getSecurityKey(): String? = when(this) {
    is Account.V1 -> null
    is Account.V2 -> securityKey
    is Account.V3 -> null
}

internal fun Account.getInstanceId(): String? = when(this) {
    is Account.V1 -> null
    is Account.V2 -> instanceId
    is Account.V3 -> instanceId
}

internal fun Account.getVersion(): Int = when(this) {
    is Account.V1 -> API_VERSION_1
    is Account.V2 -> API_VERSION_2
    is Account.V3 -> API_VERSION_3
}

internal fun Account.getAdditionalUsers(localState: TicketsDto?): List<UserDataDto>? = when(this) {
    is Account.V1 -> null
    is Account.V2 -> null
    is Account.V3 -> users.map { user -> mapToUserDataDto(localState, user) }
}

private fun mapToUserDataDto(localState: TicketsDto?, user: User): UserDataDto = UserDataDto(
    appId = user.appId,
    userId = user.userId,
    securityKey = null,
    lastNoteId = calcLastNoteId(localState, user.userId)
)