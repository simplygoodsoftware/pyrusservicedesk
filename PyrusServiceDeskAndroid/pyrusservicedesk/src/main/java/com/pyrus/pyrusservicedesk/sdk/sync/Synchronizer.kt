package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.drain
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.sync.SyncMapper.mapToGetFeedRequest
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

internal class Synchronizer(
    private val api: ServiceDeskApi,
    private val localTicketsStore: LocalTicketsStore,
    private val accountStore: AccountStore,
    private val resourceManager: AppResourceManager,
    private val idStore: IdStore,
) : CoroutineScope {

    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    override val coroutineContext: CoroutineContext = newSingleThreadContext(TAG) +
        SupervisorJob() +
        CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            PLog.e(TAG, "sync global error: ${throwable.message}")
            throwable.printStackTrace()
        }

    private val isRunning: AtomicBoolean = AtomicBoolean(false)

    private val syncLoopRequestQueue = LinkedBlockingQueue<SyncReqRes>()

    private val failDelayCounter = FailDelayCounter()


    /**
     * Sync local state and server state
     */
    suspend fun syncData(
        request: SyncRequest.Data,
    ): Try<TicketsDto> = suspendCoroutine { continuation ->
        val dataReqRes = SyncReqRes.Data(request, continuation)
        syncLoopRequestQueue.add(dataReqRes)
        tryLoop()
    }

    /**
     * Sync local state and server state
     * And send command
     */
    suspend fun syncCommand(
        request: SyncRequest.Command,
    ): Try<TicketCommandResultDto> = suspendCoroutine { continuation ->
        val syncReqRes = SyncReqRes.CommandWithContinuation(request, continuation)
        syncLoopRequestQueue.add(syncReqRes)
        tryLoop()
    }


    private fun tryLoop() {
        if (isRunning.getAndSet(true)) return
        val syncRequests = syncLoopRequestQueue.drain(MAX_COMMANDS_PER_SYNC)
        if (syncRequests.isEmpty()) {
            isRunning.set(false)
            return
        }
        runLoop(syncRequests)
    }

    private fun runLoop(syncRequests: List<SyncReqRes>) = launch {
        
        val account = accountStore.getAccount()
        val getTicketsRequest = mapToGetFeedRequest(syncRequests, localTicketsStore.getTickets(), account, resourceManager)
        val getTicketsTry = api.getTickets(getTicketsRequest)
        
        if (getTicketsTry.isSuccess()) {
            
            val commandsResults = getTicketsTry.value.commandsResult ?: emptyList()
            val commandsResultsById = commandsResults.associateBy { it.commandId }
            val commandRequests = syncRequests.filterIsInstance<SyncReqRes.CommandWithContinuation>()
            for (request in commandRequests) {
                val commandId = request.request.commandId

                val result = commandsResultsById[commandId]
                val tryResult = when {
                    result == null -> Try.Failure(Exception("result: $commandId not found")) // TODO needs to crash application
                    result.error != null -> Try.Failure(Exception("commandId: $commandId, error: ${result.error}"))
                    else -> Try.Success(result)
                }

                if (tryResult.isSuccess() && request.request is SyncRequest.Command.CreateComment) {
                    if (request.request.ticketId <= 0 && tryResult.value.ticketId != null) {
                        idStore.addTicketIdPair(request.request.ticketId, tryResult.value.ticketId)
                    }
                    if (tryResult.value.commentId != null) {
                        idStore.addCommentIdPair(request.request.localId, tryResult.value.commentId)
                    }
                }

                request.continuation.resume(tryResult)
            }

            val newState = localTicketsStore.applyDiff(getTicketsTry.value)
            val dataRequests = syncRequests.filterIsInstance<SyncReqRes.Data>()
            for (request in dataRequests) {
                request.continuation.resume(Try.Success(newState))
            }
            val hasMore = getTicketsTry.value.hasMore ?: false
            onSucceedLoopEnd(hasMore)
        }
        else {
            getTicketsTry.error.printStackTrace()
            val getRequests = syncRequests.filterIsInstance<SyncReqRes.Data>()
            for (request in getRequests) request.continuation.resume(getTicketsTry)
            
            val commandRequests = syncRequests.filterIsInstance<SyncReqRes.CommandWithContinuation>()
            onFailedLoopEnd(commandRequests)
        }
        
    }

    private fun onSucceedLoopEnd(hasMore: Boolean) {
        failDelayCounter.clear()
        val syncRequests = syncLoopRequestQueue.drain(MAX_COMMANDS_PER_SYNC)
        if (!hasMore && syncRequests.isEmpty()) {
            isRunning.set(false)
        }
        else {
            runLoop(syncRequests)
        }
    }

    private suspend fun onFailedLoopEnd(commandRequests: List<SyncReqRes.CommandWithContinuation>) {
        val delay = failDelayCounter.getNextDelay()
        delay(delay)
        val maxElements = max(MAX_COMMANDS_PER_SYNC - commandRequests.size, 0)
        val syncRequests = commandRequests + syncLoopRequestQueue.drain(maxElements)
        runLoop(syncRequests)
    }


    companion object {
        private const val MAX_COMMANDS_PER_SYNC = 50

        private const val TAG = "SyncRepository"
    }

}