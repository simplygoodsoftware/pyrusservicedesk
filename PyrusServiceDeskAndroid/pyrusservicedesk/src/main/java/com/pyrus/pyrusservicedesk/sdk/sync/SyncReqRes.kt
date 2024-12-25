package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import kotlin.coroutines.Continuation

internal sealed interface SyncReqRes {

    val request: SyncRequest

    data class Command(
        override val request: SyncRequest.Command,
        val continuation: Continuation<Try<TicketCommandResultDto>>,
    ) : SyncReqRes

    data class Data(
        override val request: SyncRequest.Data,
        val continuation: Continuation<Try<TicketsDto>>,
    ) : SyncReqRes
}