package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support

import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketHeaderEntity

internal class CommandWithHeader(
    val header: TicketHeaderEntity,
    val command: CommandWithAttachmentsEntity,
) {
}