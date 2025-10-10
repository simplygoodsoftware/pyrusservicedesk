package com.pyrus.pyrusservicedesk.sdk.updates

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_HOUR
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeature
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Class for recurring requesting data.
 * Exposes notifications of unread ticket count changes, of new reply from support received.
 * Also exposes result of requesting data.
 *
 * Subscription types: [LiveUpdateSubscriber], [NewReplySubscriber], [OnUnreadTicketCountChangedSubscriber]
 *
 * @param repository Service desk repository.
 * @param preferencesManager Manager of shared preferences.
 * @param userId Id of current user. May by null.
 */
internal class LiveUpdates(
    private val preferencesManager: Preferences,
) {

    private val coreScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        Log.e(TAG, "coreScope global error: ${throwable.message}")
        PLog.e(TAG, "coreScope global error: ${throwable.message}")
        throwable.printStackTrace()
    })

    private var lastActiveTime: Long = preferencesManager.getLastActiveTime()
    private var activeScreenCount = 0
    private var lastCommentId: Long? = null

    // notified in UI thread
    private val dataSubscribers = mutableSetOf<LiveUpdateSubscriber>()
    private val newReplySubscribers = mutableSetOf<NewReplySubscriber>()
    private val ticketCountChangedSubscribers = mutableSetOf<OnUnreadTicketCountChangedSubscriber>()

    private var recentUnreadCounter = -1

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStarted = false
    private var replyIsShown = false

    var updateFeature: AutoRefreshFeature? = null

    private var replayJob: Job? = null

    private val ticketsUpdateRunnable = object : Runnable {
        override fun run() {
            //val requestUserId = userId
//            replayJob =
//            GlobalScope.launch(ioDispatcher) {
//                repository.sync()
//                if (ticketsTry.isSuccess()) {
//                    val data = ticketsTry.value
//                    val newUnread = data.count()//data.count { !it.isRead } //TODO
//                    this@launch.launch(mainDispatcher) {
//                        // TODO
//                        if (true) return@launch
////                        val userId = PyrusServiceDesk.get().userId
//                        if (data.isEmpty())
//                            stopUpdates()
//                        else if (requestUserId == userId)
//                            processGetTicketsSuccess(data, newUnread)
//                    }
//                }
//                else {
//                    PLog.d(TAG, "ticketsUpdateRunnable, onFailure")
//                }
            //}
//            val interval = getTicketsUpdateInterval(lastActiveTime)
//            PLog.d(TAG, "ticketsUpdateRunnable, interval: $interval")
//            if (interval == -1L) {
//                stopUpdates()
//                return
//            }
//            mainHandler.postDelayed(this, interval)
        }
    }

    /**
     * Registers [subscriber] to on new reply events
     */

    @MainThread
    fun subscribeOnReply(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "subscribeOnReply")
        newReplySubscribers.add(subscriber)
        preferencesManager.saveLastActiveTime(System.currentTimeMillis())
        onSubscribe()
        val localTicketsStore = injector().localTicketsStore
        coreScope.launch(Dispatchers.IO) {
            val lastComment = localTicketsStore.getTickets().lastOrNull()
            if (lastComment != null && lastComment.isRead != true) {
                notifyNewReplySubscriber(subscriber, lastComment)
            }
        }
    }

    /**
     * Unregisters [subscriber] from new reply events
     */
    @MainThread
    fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "unsubscribeFromReplies")
        newReplySubscribers.remove(subscriber)
        updateFeature?.cancel()
    }

    /**
     * Registers [subscriber] on unread ticket count changed events
     */
    @MainThread
    internal fun subscribeOnUnreadTicketCountChanged(subscriber: OnUnreadTicketCountChangedSubscriber) {
        ticketCountChangedSubscribers.add(subscriber)
        onSubscribe()
    }

    /**
     * Unregisters [subscriber] from unread ticket count changed events
     */
    @MainThread
    internal fun unsubscribeFromTicketCountChanged(subscriber: OnUnreadTicketCountChangedSubscriber) {
        ticketCountChangedSubscribers.remove(subscriber)
        onUnsubscribe()
    }

    /**
     * Registers [liveUpdateSubscriber] on new data received event
     */
    @MainThread
    internal fun subscribeOnData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.add(liveUpdateSubscriber)
        onSubscribe()
    }

    /**
     * Unregisters [liveUpdateSubscriber] from new data received event
     */
    @MainThread
    internal fun unsubscribeFromData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.remove(liveUpdateSubscriber)
        onUnsubscribe()
    }

    internal fun reset(userId: String?) {
        PLog.d(TAG, "reset")
        //this.userId = userId
        lastCommentId = 0
        preferencesManager.saveLastActiveTime(-1)
        preferencesManager.removeLastComment()
    }

    /**
     * Start tickets update if it is not already running.
     */
    internal fun updateGetTicketsIntervalIfNeeded(lastActiveTime: Long) {
        val currentLastActiveTime = preferencesManager.getLastActiveTime()
        val maxLastActiveTime = max(currentLastActiveTime, lastActiveTime)
        PLog.d(TAG, "startUpdatesIfNeeded, lastActiveTime: $lastActiveTime, currentLastActiveTime: $currentLastActiveTime")
        if (maxLastActiveTime > currentLastActiveTime) {
            PLog.d(TAG, "startUpdatesIfNeeded, write last active time in preferences, time: $maxLastActiveTime")
            preferencesManager.saveLastActiveTime(maxLastActiveTime)
        }
        this.lastActiveTime = maxLastActiveTime

        val interval = getTicketsUpdateInterval(maxLastActiveTime)
        val currentInterval = getTicketsUpdateInterval(currentLastActiveTime)
        PLog.d(TAG, "startUpdatesIfNeeded, " +
                "interval: $interval, " +
                "currentInterval: $currentInterval, " +
                "system time: ${System.currentTimeMillis()}"
        )
        if (interval == currentInterval && isStarted)
            return
        PLog.d(TAG, "startUpdatesIfNeeded, change interval, isStarted $isStarted")
        if (isStarted)
            stopUpdates()
        if (interval != -1L || activeScreenCount > 0)
            startUpdates()
    }

    /**
     * Reset lastComment
     */
    internal fun onReadComments() {
        val lastComment = preferencesManager.getLastComment()
        if (lastComment != null)
            preferencesManager.saveLastComment(lastComment.copy(isShown = true, isRead = true))
    }

    /**
     * Increase active screen count by one.
     */
    internal fun increaseActiveScreenCount() {
        activeScreenCount++
        PLog.d(TAG, "increaseActiveScreenCount, activeScreenCount: $activeScreenCount")
        updateGetTicketsIntervalIfNeeded(preferencesManager.getLastActiveTime())
    }

    /**
     * Decrease active screen count by one.
     */
    internal fun decreaseActiveScreenCount() {
        activeScreenCount--
        PLog.d(TAG, "decreaseActiveScreenCount, activeScreenCount: $activeScreenCount")
        updateGetTicketsIntervalIfNeeded(preferencesManager.getLastActiveTime())
    }

    private fun onSubscribe() {
        updateGetTicketsIntervalIfNeeded(preferencesManager.getLastActiveTime())
    }

    private fun onUnsubscribe() {
        if (!isStarted)
            return

        if (newReplySubscribers.isEmpty()
            && ticketCountChangedSubscribers.isEmpty()
            && dataSubscribers.isEmpty()
        )
            stopUpdates()
    }

    private fun getTicketsUpdateInterval(lastActiveTime: Long): Long {
        val diff = System.currentTimeMillis() - lastActiveTime
        return when {
            diff < 1.5 * MILLISECONDS_IN_MINUTE -> 5L * MILLISECONDS_IN_SECOND
            diff < 5 * MILLISECONDS_IN_MINUTE -> 15L * MILLISECONDS_IN_SECOND
            diff < MILLISECONDS_IN_HOUR || activeScreenCount > 0 * MILLISECONDS_IN_SECOND -> MILLISECONDS_IN_MINUTE.toLong()
            diff < 3 * MILLISECONDS_IN_DAY -> 3L * MILLISECONDS_IN_MINUTE
            else -> -1L
        }
    }

    @MainThread
    private fun startUpdates() {
        PLog.d(TAG, "startUpdates")
        isStarted = true
        val repository = injector().repository
        replayJob = coreScope.launch(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                repository.sync()
                val lastActiveTime = preferencesManager.getLastActiveTime()
                val interval = getTicketsUpdateInterval(lastActiveTime)
                PLog.d(TAG, "ticketsUpdateRunnable, interval: $interval")
                if (interval == -1L) {
                    stopUpdates()
                    replayJob?.cancel()
                }
                delay(interval)
            }
        }
    }

    private fun stopUpdates() {
        PLog.d(TAG, "stopUpdates")
        isStarted = false
        replayJob?.cancel()
    }

    /*@MainThread
    private fun processGetTicketsSuccess(ticketsInfo: TicketsInfo, newUnreadCount: Int) {
        val isChanged = recentUnreadCounter != newUnreadCount
        PLog.d(TAG, "processSuccess, isChanged: $isChanged, recentUnreadCounter: $recentUnreadCounter, newUnreadCount: $newUnreadCount")
        notifyDataSubscribers(ticketsInfo, isChanged, newUnreadCount)
        notifyUnreadCountSubscribers(isChanged, newUnreadCount)
        recentUnreadCounter = newUnreadCount

        val lastSavedComment = preferencesManager.getLastComment()
        val hasUnreadTickets = newUnreadCount > 0

        val lastCommentId = ticketsInfo.firstOrNull()?.lastComment?.commentId ?: return


        if (lastSavedComment != null && lastSavedComment.id == lastCommentId && !lastSavedComment.isRead && !hasUnreadTickets) {
            val chatIsShown = activeScreenCount > 0

            val lastComment = lastSavedComment.copy(isShown = newReplySubscribers.isNotEmpty() || chatIsShown, isRead = true)
            preferencesManager.saveLastComment(lastComment)
            if (!chatIsShown)
                notifyNewReplySubscribers(lastComment)
        }
        else if (!hasUnreadTickets && !firstReplyIsShown) {
            if (activeScreenCount <= 0)
                notifyNewReplySubscribers(LastComment(0, true, false, null, null, 0, 0))
        }
        else if ((lastSavedComment?.id ?: 0) < lastCommentId && this.lastCommentId < lastCommentId) {
            this.lastCommentId = lastCommentId

            val responseUserId = userId
            PLog.d(TAG, "getTicketRequest")
            for (ticket in ticketsInfo) {

//                coreScope.launch(ioDispatcher) {
//                    val feedTry = repository.getFeed(ticket.ticketId, ticket.userId ?: "", true) //TODO это удалится вообще?
//
//                    withContext(mainDispatcher) {
//                        if (responseUserId != userId) {
//                            return@withContext
//                        }
//
//                        if (!feedTry.isSuccess()) {
//                            this@LiveUpdates.lastCommentId = 0
//                            PLog.d(TAG, "response.hasError, error: ${feedTry.error}")
//                            return@withContext
//                        }
//
//                        val comments = feedTry.value.comments
//                        val lastSavedCommentInMainScope =
//                            this@LiveUpdates.preferencesManager.getLastComment()
//                        val lastServerComment =
//                            comments.findLast { !it.isInbound } ?: return@withContext
//                        if (lastServerComment.id <= (lastSavedCommentInMainScope?.id ?: 0))
//                            return@withContext
//
//                        val lastUserComment =
//                            comments.findLast { it.isInbound } ?: return@withContext
//
//                        updateGetTicketsIntervalIfNeeded(lastUserComment.creationTime)
//
//                        val chatIsShown = activeScreenCount > 0
//                        val lastComment = LastComment.mapFromComment(
//                            newReplySubscribers.isNotEmpty() || chatIsShown,
//                            !hasUnreadTickets,
//                            lastServerComment
//                        )
//
//                        this@LiveUpdates.preferencesManager.saveLastComment(lastComment)
//                        if (!chatIsShown)
//                            notifyNewReplySubscribers(lastComment)
//                    }
//                }

            }
        }
    }*/

    private fun notifyUnreadCountSubscribers(isChanged: Boolean, newUnreadCount: Int) {
        if (isChanged) {
            val hasNewComments = newUnreadCount > 0
            PLog.d(TAG, "notifyUnreadCountSubscribers, hasNewComments: $hasNewComments, activeScreenCount: $activeScreenCount")
            ticketCountChangedSubscribers.forEach { it.onUnreadTicketCountChanged(newUnreadCount) }
        }
    }

    private fun notifyDataSubscribers(ticketsInfo: TicketsInfo, isChanged: Boolean, newUnreadCount: Int) {
        dataSubscribers.forEach {
            //it.onNewData(ticketsInfo)
            if (isChanged)
                it.onUnreadTicketCountChanged(newUnreadCount)
        }
    }

    fun notifyNewReplySubscribers(localTicketsStore: LocalTicketsStore) {
        newReplySubscribers.forEach {
            coreScope.launch(Dispatchers.IO) {
                val lastComment = localTicketsStore.getTickets().lastOrNull()
                val needRoNotify =
                    (lastCommentId != lastComment?.lastComment?.commentId && lastComment?.isRead == false)
                        || lastComment?.isRead != replyIsShown
                if (lastComment != null && needRoNotify) {
                    notifyNewReplySubscriber(it, lastComment)
                }
            }
        }
    }

    private fun notifyNewReplySubscriber(subscriber: NewReplySubscriber, lastComment:  TicketEntity) {
        PLog.d(TAG, "notifyNewReplySubscriber, comment: $lastComment")
        replyIsShown = lastComment.isRead == true
        lastCommentId = lastComment.lastComment?.commentId
        val hasNewComments = lastComment.isRead != true
        coreScope.launch(Dispatchers.Main) {
            subscriber.onNewReply(
                hasNewComments,
                if (hasNewComments) lastComment.lastComment?.body else null,
                if (hasNewComments && lastComment.lastComment?.lastAttachmentName != null) 1 else 0, //TODO kate check if we need more then 1 attachment
                if (hasNewComments && lastComment.lastComment?.lastAttachmentName != null) listOf(
                    lastComment.lastComment.lastAttachmentName
                )
                else null,
                if (hasNewComments && lastComment.lastComment?.creationDate != null) lastComment.lastComment.creationDate else 0
            )
        }
    }

    companion object {
        private val TAG = LiveUpdates::class.java.simpleName
    }

}

/**
 * Interface for observing updates of data.
 */
internal interface LiveUpdateSubscriber: OnUnreadTicketCountChangedSubscriber {
    /**
     * Invoked when new portion of [tickets] data is received.
     */
    fun onNewData(tickets: List<TicketDto>)
}

/**
 * Interface for observing changes of unread tickets count.
 */
internal interface OnUnreadTicketCountChangedSubscriber{
    /**
     * Invoked when count of unread tickets is changed.
     */
    fun onUnreadTicketCountChanged(unreadTicketCount: Int)
}