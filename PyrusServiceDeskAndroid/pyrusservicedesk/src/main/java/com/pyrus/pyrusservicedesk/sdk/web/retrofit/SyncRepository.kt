package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.call_adapter.TryCallAdapterFactory
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.data.UserData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

private typealias SyncLoopRequest = Continuation<Try<Int>>

internal class SyncRepository(val retrofit: Retrofit) : CoroutineScope {

    override val coroutineContext: CoroutineContext = newSingleThreadContext("Synchronizer") +
            SupervisorJob() +
            CoroutineExceptionHandler { _, throwable ->
                PLog.e(TAG, "syncV2 global error: ${throwable.message}")
            }

    private val dispatcher: CoroutineDispatcher = Dispatchers.Default



    private val api: ServiceDeskApi
//    private val _stateFlow = MutableStateFlow("Initial value")
//    val ticketsListStateFlow: StateFlow<String> get() = _stateFlow
//    fun updateValue(newValue: String) {
//        _stateFlow.value = newValue
//    }
//
    private val _ticketsListStateFlow = MutableStateFlow(
        SyncRes(
            Tickets(false, emptyList(), emptyList(), emptyList())
        )
    )
    val ticketsListStateFlow: StateFlow<SyncRes> get() = _ticketsListStateFlow

    fun updateValue(res: SyncRes) {
        _ticketsListStateFlow.value = res
    }

    init {
        api = retrofit.create(ServiceDeskApi::class.java)
    }

    class SyncTryRes(
        val tryRes: Try<Tickets>
    )

    class SyncRes(
        val tickets: Tickets,
    )

    private val syncLoopRequestQueue = LinkedBlockingQueue<SyncLoopRequest>()

    private val isRunning = AtomicBoolean(false)

    private val pendingSyncReq = AtomicBoolean(false)

    fun startSync() {
        PLog.d(TAG, "startSync")
        pendingSyncReq.set(true)
        tryLoop()
    }

    suspend fun syncLoop(): Try<Int> = suspendCoroutine { continuation ->
        syncLoopRequestQueue.add(continuation)
        tryLoop()
    }

    fun <T> BlockingQueue<T>.drain(): List<T> {
        val res = ArrayList<T>()
        drainTo(res)
        return res
    }

    fun <T> Collection<Continuation<T>>.resume(value: T) {
        for (continuation in this) {
            continuation.resume(value)
        }
    }

    private fun tryLoop() {
        if (isRunning.getAndSet(true)) {
            PLog.d(TAG, "tryLoop, sync is already running")
            return
        }
        val syncRequests = syncLoopRequestQueue.drain()


        pendingSyncReq.set(false)

        PLog.d(TAG, "tryLoop, start loop")
        runLoop(syncRequests)
    }

    private fun onEndLoop(hasMore: Boolean) {
        val syncRequests = syncLoopRequestQueue.drain()

        val hasPendingSyncReq = pendingSyncReq.getAndSet(false)

        if (!hasMore
            && syncRequests.isEmpty()
            && hasPendingSyncReq.not()
        ) {
            PLog.d(TAG, "onEndLoop exit sync loops")
            isRunning.set(false)
        }
        else {
            PLog.d(TAG, "onEndLoop go to next loop")
            runLoop(syncRequests)
        }
    }

        private fun runLoop(
            syncRequests: List<Continuation<Try<Int>>>
        ) = launch {

        val hasFiles = false//syncEventStore.hasFilesForUpload()
        PLog.d(TAG, "SyncThread.handleMessage, hasFiles: %b", hasFiles)
        if (hasFiles) {
            PLog.d(TAG, "SyncThread.handleMessageHave, starting files upload")
            //TODO syncFilesUpload()
        }

        //val syncEvents = syncEventStore.syncEvents

        //PLog.i(TAG, "runLoop syncEvents: ${syncEvents.contentToString()}")

        val request = createSyncRequest()
        val syncRes: Try<SyncRes> = sync(request)

        when (syncRes) {
            is Try.Failure -> {
                PLog.d(TAG, "runLoop, Failure")
                val e = syncRes.error
                //e.logAsNetwork(TAG, "runLoop, sync error: $e")
                syncRequests.resume(Try.Failure(syncRes.error))

                onEndLoop(false)
            }
            is Try.Success -> {
                PLog.d(TAG, "runLoop, Success")

                syncRequests.resume(Try.Success(request.version))

                //onEndLoop(syncRes.value.tickets.hasMore ?: false)
            }
        }
        sendSyncComplete(syncRes)
    }

    internal suspend fun sync(
        request: RequestBodyBase,
    ): Try<SyncRes> {

        val responseTry: Try<Tickets> = try {
                api.getTickets(request)
            } catch (e: Exception) {
                e.printStackTrace()
                return Try.Failure(e)
            }


//        val responseTry = try {
//            api.getTickets(request)
//        }
//        catch (e: Exception) {
//            //e.logAsNetwork(TAG, "sync error: $e")
//            return Try.Failure(e)
//        }

        val responseData = when (responseTry) {
            is Try.Success -> responseTry.value
            is Try.Failure -> {
                PLog.d(TAG, "sync server error: ${responseTry.error}")
                return Try.Failure(responseTry.error)
            }
        }

        return Try.Success(
            SyncRes(
                responseData,
            )
        )
    }

    private fun sendSyncComplete(syncTry: Try<SyncRes>) {

        when(syncTry) {
            is Try.Failure -> {
                //onNewData
            }

            is Try.Success -> {
                updateValue(syncTry.value)
                //updateValue(Random.nextInt(0, 100).toString())
                //TODO update
            }
        }
    }

    //TODO
    internal fun createSyncRequest(): RequestBodyBase {
        return RequestBodyBase(
            additionalUsers = getAdditionalUsers(),
            authorId = "10",
            authorName = "Kate Test",
            appId = "",//PyrusServiceDesk.users[0].appId,
            userId = "",//PyrusServiceDesk.users[0].userId,
            instanceId = "4F71E6BA-55F8-46EE-B281-C9E18C42224F",
            version = getVersion(),
            apiSign = apiFlag,
            securityKey = getSecurityKey()
        )
    }

    private fun getAdditionalUsers(): List<UserData> {
//        val list = PyrusServiceDesk.users.map { UserData(it.appId, it.userId, getSecurityKey()?:"") }
//        return list
        return emptyList()
    }

    private fun getVersion(): Int {
        return API_VERSION_2//PyrusServiceDesk.get().apiVersion //TODO API_VERSION_1 commands don't work
    }

    private fun getSecurityKey(): String? {
        //if (getVersion() == API_VERSION_2)
            //return PyrusServiceDesk.get().securityKey
        return null
    }

    companion object {
        private const val apiFlag = "AAAAAAAAAAAU"
        private const val TAG = "SyncRepository"
    }
}