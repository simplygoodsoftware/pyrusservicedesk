package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import kotlin.coroutines.Continuation

internal sealed interface SyncReqRes {

    val request: SyncRequest

    class Command(
        override val request: SyncRequest.Command,
    ) : SyncReqRes

    class CommandWithContinuation(
        override val request: SyncRequest.Command,
        override val continuation: Continuation<Try<TicketCommandResultDto>>,
    ) : SyncReqRes, WithContinuation<TicketCommandResultDto>

    class Data(
        override val request: SyncRequest.Data,
        override val continuation: Continuation<Try<TicketsDto>>,
    ) : SyncReqRes, WithContinuation<TicketsDto>
}

internal interface WithContinuation<T> {
    val continuation: Continuation<Try<T>>
}