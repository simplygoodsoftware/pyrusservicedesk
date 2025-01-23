package com.pyrus.pyrusservicedesk._ref.data.multy_chat

import com.pyrus.pyrusservicedesk._ref.data.TicketHeader

internal data class TicketSetInfo(
    val appId: String,
    val userIds: Set<String>,
    val orgName: String,
    val orgLogoUrl: String?,
    val tickets: List<TicketHeader>,
)