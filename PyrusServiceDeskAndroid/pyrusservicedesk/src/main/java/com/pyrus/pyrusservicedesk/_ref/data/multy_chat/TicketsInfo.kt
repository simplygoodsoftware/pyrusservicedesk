package com.pyrus.pyrusservicedesk._ref.data.multy_chat

import com.pyrus.pyrusservicedesk.core.Account

internal data class TicketsInfo(
    val account: Account,
    val ticketSetInfoList: List<TicketSetInfo>,
)