package com.pyrus.pyrusservicedesk._ref.data.multy_chat

import com.pyrus.pyrusservicedesk.User

data class TicketsInfo(
    val usersWithData: List<User>,
    val users: List<User>,
    val ticketSetInfoList: List<TicketSetInfo>,
)