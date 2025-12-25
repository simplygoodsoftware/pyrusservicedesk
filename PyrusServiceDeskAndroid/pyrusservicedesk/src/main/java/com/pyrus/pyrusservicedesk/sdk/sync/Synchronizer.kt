package com.pyrus.pyrusservicedesk.sdk.sync

import android.util.Log
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_1
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.call_adapter.HttpException
import com.pyrus.pyrusservicedesk._ref.utils.drain
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.core.getAppId
import com.pyrus.pyrusservicedesk.core.getExtraUsers
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.core.getVersion
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.sync.SyncMapper.mapToGetFeedRequest
import com.pyrus.pyrusservicedesk.sdk.updates.Preferences
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

internal class Synchronizer(
    private val api: ServiceDeskApi,
    private val localTicketsStore: LocalTicketsStore,
    private val accessDeniedEventBus: AccessDeniedEventBus,
    private val accountStore: AccountStore,
    private val resourceManager: AppResourceManager,
    private val idStore: IdStore,
    private val commandsStore: LocalCommandsStore,
    private val preferences: Preferences,
) : CoroutineScope {

    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    override val coroutineContext: CoroutineContext = newSingleThreadContext(TAG) +
        SupervisorJob() +
        CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            Log.e(TAG, "sync global error: ${throwable.message}")
            PLog.e(TAG, "sync global error: ${throwable.message}")
            throwable.printStackTrace()
        }

    private val isRunning: AtomicBoolean = AtomicBoolean(false)

    private val syncLoopRequestQueue = LinkedBlockingQueue<SyncReqRes>()

    private val failDelay = FailDelay()


    /**
     * Sync local state and server state
     */
    suspend fun syncData(
        request: SyncRequest.Data,
        cancelTimeout: Boolean,
    ): Try<TicketsDto> = suspendCoroutine { continuation ->
        if (cancelTimeout) failDelay.cancel()
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
        val syncRequests = getQueuedCommands(MAX_COMMANDS_PER_SYNC)
        if (syncRequests.isEmpty()) {
            isRunning.set(false)
            return
        }
        runLoop(syncRequests)
    }

    private fun runLoop(syncRequests: List<SyncReqRes>) = launch {
        
        val account = accountStore.getAccount()

        val firstUserId: String? = account.getUserId()
        val firstAppId: String? = account.getAppId()
        if (firstUserId == null || firstAppId == null) {
            onLoopDeadEnd(syncRequests)
            return@launch
        }

        val reloadCacheVersion = preferences.getReloadCacheVersion()

        val tickets = when {
            reloadCacheVersion >= RELOAD_CACHE_VERSION -> localTicketsStore.getTickets()
            else -> emptyList()
        }

        val modifiedRequests = modifyRequests(syncRequests)
        val getTicketsRequest = mapToGetFeedRequest(
            syncRequests = modifiedRequests,
            tickets = tickets,
            account = account,
            resourceManager = resourceManager,
            firstUserId = firstUserId,
            firstAppId = firstAppId,
        )

        val getTicketsTry = api.getTickets(getTicketsRequest)
        
        if (getTicketsTry.isSuccess()) {

            if (reloadCacheVersion < RELOAD_CACHE_VERSION) {
                preferences.saveReloadCacheVersion(RELOAD_CACHE_VERSION)
            }
            
            val commandsResults = getTicketsTry.value.commandsResult ?: emptyList()
            val commandsResultsById = commandsResults.associateBy { it.commandId }

            getTicketsTry.value.authorAccessDenied?.let {
                val users = account.getUsers()
                val extra = account.getExtraUsers()
                val accessDeniedUsers = users.filter { user -> user.userId in it && user !in extra}
                if (accessDeniedUsers.isNotEmpty()) {
                    accessDeniedEventBus.post(accessDeniedUsers)
                }
            }

            if (getTicketsTry.value.authorAccessDenied != null) {
                accountStore.removeUsers(getTicketsTry.value.authorAccessDenied)
                val changeUsersIntent = ConfigUtils.getChangeUsersIntent()
                if (changeUsersIntent != null) {
                    try {
                        changeUsersIntent.send()
                    } catch (e: Exception) {
                        Log.d(TAG, e.toString())
                    }
                }
            }

            val changeUsersIntent = ConfigUtils.getChangeUsersIntent()
            accountStore.cleanExtraUsers()
            getTicketsTry.value.applications?.forEach { app ->
                app.extraUsers?.forEach {
                    if (it.userId != null && app.appId != null && it.title != null) {
                        val user = User(it.userId, app.appId, it.title)
                        accountStore.addUser(user)
                        accountStore.addExtraUser(user)
                        changeUsersIntent?.let {
                            try {
                                changeUsersIntent.send()
                            } catch (e: Exception) {
                                Log.d(TAG, e.toString())
                            }
                        }
                    }
                }
            }

            val authorAccessDenied = getTicketsTry.value.authorAccessDenied ?: emptyList()
            val usersWithData = account.getUsers().filter { it.userId !in authorAccessDenied }

            localTicketsStore.storeServerState(usersWithData, getTicketsTry.value)

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
                        commandsStore.updateCommandsTicketId(request.request.ticketId, tryResult.value.ticketId)
                    }
                    if (tryResult.value.commentId != null) {
                        idStore.addCommentIdPair(request.request.localId, tryResult.value.commentId)
                    }
                }

                request.continuation.resume(tryResult)
            }

            val dataRequests = syncRequests.filterIsInstance<SyncReqRes.Data>()
            for (request in dataRequests) {
                request.continuation.resume(getTicketsTry)
            }
            onSucceedLoopEnd()
        }
        else {
            getTicketsTry.error.printStackTrace()
            val statusCode = (getTicketsTry.error as? HttpException)?.statusCode
            if (statusCode == FAILED_AUTHORIZATION_ERROR_CODE || statusCode == FAILED_AUTHORIZATION_ERROR_CODE_FORBIDDEN) {
                withContext(Dispatchers.Main) {
                    PyrusServiceDesk.onAuthorizationFailed?.run()
                }
                failDelay.clear()
                val getRequests = syncRequests.filterIsInstance<SyncReqRes.Data>()
                for (request in getRequests) request.continuation.resume(getTicketsTry)
                isRunning.set(false)
                return@launch
            }
            val getRequests = syncRequests.filterIsInstance<SyncReqRes.Data>()
            for (request in getRequests) request.continuation.resume(getTicketsTry)
            
            val commandRequests = syncRequests.filterIsInstance<SyncReqRes.CommandWithContinuation>()
            onFailedLoopEnd(commandRequests)
        }
        
    }

    private fun onSucceedLoopEnd() {
        failDelay.clear()
        val syncRequests = getQueuedCommands(MAX_COMMANDS_PER_SYNC)
        if (syncRequests.isEmpty()) {
            isRunning.set(false)
        }
        else {
            runLoop(syncRequests)
        }
    }

    private fun onLoopDeadEnd(commandRequests: List<SyncReqRes>) {
        val withContinuation = commandRequests.filterIsInstance(WithContinuation::class.java)
        for (continuation in withContinuation) {
            continuation.continuation.resume(Try.Failure(Exception("users not found")))
        }
        isRunning.set(false)
    }

    private suspend fun onFailedLoopEnd(commandRequests: List<SyncReqRes.CommandWithContinuation>) {
        failDelay.cancelableDelay()
        val maxElements = max(MAX_COMMANDS_PER_SYNC - commandRequests.size, 0)
        val syncRequests = commandRequests + getQueuedCommands(maxElements)
        runLoop(syncRequests)
    }

    private fun getQueuedCommands(maxElements: Int): List<SyncReqRes> {
        return syncLoopRequestQueue.drain(maxElements)
    }

    private fun modifyRequests(syncRequests: List<SyncReqRes>): List<SyncReqRes> {
        val requestNewTicketIds = hashSetOf<Long>()

        return syncRequests.map { syncReqRes ->
            val req = syncReqRes.request
            if (req !is SyncRequest.Command.CreateComment) return@map syncReqRes

            if ((accountStore.getAccount().getVersion() == API_VERSION_1
                    ||accountStore.getAccount().getVersion() == API_VERSION_2)
                && localTicketsStore.getTickets().lastOrNull()?.isActive == false
                && req.rating == null
                && req.ratingComment == null
            ) {
                val newRequest = req.copy(ticketId = commandsStore.getNextLocalId(), requestNewTicket = true)
                return@map copySyncReqRes(syncReqRes, newRequest)
            }


            if (req.ticketId > 0)  return@map syncReqRes

            val serverId = idStore.getTicketServerId(req.ticketId)
            if (serverId != null && serverId > 0L) {
                val newRequest = req.copy(ticketId = serverId)
                return@map copySyncReqRes(syncReqRes, newRequest)
            }

            if (req.ticketId in requestNewTicketIds) return@map syncReqRes

            requestNewTicketIds.add(req.ticketId)
            val newRequest = req.copy(requestNewTicket = true)
            preferences.setCreatedTicketsCount(preferences.getCreatedTicketsCount() + 1)
            copySyncReqRes(syncReqRes, newRequest)
        }
    }

    private fun copySyncReqRes(syncReqRes: SyncReqRes, request: SyncRequest.Command): SyncReqRes {
        return when (syncReqRes) {
            is SyncReqRes.Command -> SyncReqRes.Command(
                request = request
            )

            is SyncReqRes.CommandWithContinuation -> SyncReqRes.CommandWithContinuation(
                request = request,
                continuation = syncReqRes.continuation
            )

            else -> syncReqRes
        }
    }


    companion object {
        private const val RELOAD_CACHE_VERSION = 2_007_001L
        private const val MAX_COMMANDS_PER_SYNC = 50
        const val FAILED_AUTHORIZATION_ERROR_CODE_FORBIDDEN = 403
        const val FAILED_AUTHORIZATION_ERROR_CODE = 400
        private const val TAG = "SyncRepository"
    }

}