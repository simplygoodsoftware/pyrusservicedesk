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
import com.pyrus.pyrusservicedesk.sdk.data.ApplicationDto
import com.pyrus.pyrusservicedesk.sdk.data.AuthorDto
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.sync.SyncMapper.mapToGetFeedRequest
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.random.Random

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

        val getTicketsTry: Try<TicketsDto> = fakeTickets()
        
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

    private val commentId = java.util.concurrent.atomic.AtomicLong(1L)
    private val counter = AtomicInteger(1)

    private fun fakeTickets(): Try<TicketsDto> {

        Log.d("SDS", "counter: ${counter.get()}")

        val step = counter.getAndIncrement()

/*        Хочу прекратить списания
        Счет Иви
        За что списали деньги?
        Вопрос про Яндекс на Иви
        Вопрос по входу в аккаунт
        Вопрос по подписке
        Проблемы при просмотре видео
        Управление аккаунтом/подпиской
        Другое

        Хочу прекратить списания
        Поделиться подпиской (пригласить/принять)
        Турецкие сериалы
        Авторизоваться на другом устройстве
        Не вижу оплаченную подписку
        Управление банковской картой
        Вопрос по подписке от Яндекс Плюс
        Ошибки/трудности при просмотре видео
        Что такое счёт Иви
        Далее

        */







        val commentBody: String = when(step) {
            1 -> ""
            2, 3 -> "<button>Кнопа 1</button>" +
                "<button>Кнопа 2</button>" +
                "<button>далее</button>"
            4 -> "<button>Хочу прекратить списания</button>" +
                "<button>Счет Иви</button>" +
                "<button>За что списали деньги?</button>" +
                "<button>Вопрос про Яндекс на Иви</button>" +
                "<button>Вопрос по входу в аккаунт</button>" +
                "<button>Вопрос по подписке</button>" +
                "<button>Проблемы при просмотре видео</button>" +
                "<button>Управление аккаунтом/подпиской</button>" +
                "<button>Другое</button>"
            5 -> "<button>Хочу прекратить списания</button>" +
                "<button>Поделиться подпиской (пригласить/принять)</button>" +
                "<button>Турецкие сериалы</button>" +
                "<button>Авторизоваться на другом устройстве</button>" +
                "<button>Не вижу оплаченную подписку</button>" +
                "<button>Управление банковской картой</button>" +
                "<button>Вопрос по подписке от Яндекс Плюс</button>" +
                "<button>Ошибки/трудности при просмотре видео</button>" +
                "<button>Что такое счёт Иви</button>" +
                "<button>Далее</button>"
            else ->
                if (step % 9 == 0) "comment:::: $step"
                else {
                    val sb = StringBuilder()
                    for (i in 0 until 5) {
                        sb.append("<button>Кнопа ${step + i}</button>")
                    }
                    sb.append("<button>Далее</button>")
                    sb.toString()
                }
        }

        val addComment: Boolean = when(step) {
            1 -> true
            2 -> true
            3 -> false
            4 -> true
            5 -> true
            else -> true
        }

        val isInbound = when(step) {
            1 -> true
            else -> false
        }

        val comment = createComment(commentBody, isInbound)
        val tickets = TicketsDto(
            applications = listOf(ApplicationDto(
                appId = "0HUi7grFuWVFHqWtL3f5YD-4PYJXiOEoLfCDb2yhTthkHBpedNbNU4O01YD2OnsSpvbMiXmweUF8akomZZIW1Ilb-W9mOPuK70L4lCI1mK0dJqXYUp0l-MJlsUv9tr8dSmKCSw==",
                authorsInfo = null,
                extraUsers = null,
                orgDescription = "",
                orgLogoUrl = "/mobilelogo?p=0HUi7grFuWVFHqWtL3f5YD-4PYJXiOEoLfCDb2yhTthkHBpedNbNU4O01YD2OnsSpvbMiXmweUF8akomZZIW1Ilb-W9mOPuK70L4lCI1mK0dJqXYUp0l-MJlsUv9tr8dSmKCSw%3D%3D&v=954532399",
                orgName = "Droid Home (для тестов мобильных разработчиков Pyrus)",
                ratingSettings = null,
                welcomeMessage = "Test<br>Test<br>Test<br><a href=\"https://pyrus.com/t#fc838615/mobile-app-chat\">Ссылка</a><br><button>Пречат сообщение</button><a data-type=\"button\" href=\"https://pyrus.com/t#fc838615/mobile-app-chat\">Пречат ссылка</a><button>Пречат-2</button>",
            )),
            tickets = listOf(createFakeTicket(comment, addComment)),
            commandsResult = null,
            authorAccessDenied = null,
        )

        return Try.Success(tickets)
    }

    private fun createFakeTicket(commentDto: CommentDto, addComment: Boolean) = TicketDto(
        ticketId = 327433149,
        userId = null,
        subject = "",
        author = "",
        isRead = true,
        lastComment = CommentDto(
            commentId = commentDto.commentId,
            body = commentDto.body,
            isInbound = commentDto.isInbound,
            attachments = commentDto.attachments,
            creationDate = commentDto.creationDate,
            author = null,
            rating = commentDto.rating,
            ratingComment = commentDto.ratingComment
        ),
        comments = if (addComment) listOf(commentDto) else null,
        isActive = true,
        createdAt = 1766572502000,
        showRating = false,
        showRatingText = null
    )

    private fun createComment(body: String, isInbound: Boolean) = CommentDto(
        commentId = commentId.getAndIncrement(),
        body = body,
        isInbound = isInbound,
        attachments = null,
        creationDate = System.currentTimeMillis(),
        author = AuthorDto(
            name = "Ivan Ivanov",
            authorId = null,
            avatarId = null,
            avatarColorString = null
        ),
        rating = null,
        ratingComment = null,
    )

}