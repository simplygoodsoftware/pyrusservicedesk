package com.pyrus.pyrusservicedesk._ref.data.multy_chat

import com.pyrus.pyrusservicedesk._ref.data.FullTicket

internal data class TicketSetInfo(
    val appId: String,
    val orgName: String,
    val orgLogoUrl: String?,
    val tickets: List<FullTicket>
)