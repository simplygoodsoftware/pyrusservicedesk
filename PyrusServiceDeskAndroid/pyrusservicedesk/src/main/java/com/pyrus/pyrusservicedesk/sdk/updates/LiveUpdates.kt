package com.pyrus.pyrusservicedesk.sdk.updates

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.log.PLog
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_HOUR
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_SECOND
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Class for recurring requesting data.
 * Exposes notifications of unread ticket count changes, of new reply from support received.
 * Also exposes result of requesting data.
 *
 * Subscription types: [LiveUpdateSubscriber], [NewReplySubscriber], [OnUnreadTicketCountChangedSubscriber]
 *
 * @param requests Service desk request factory.
 * @param preferencesManager Manager of shared preferences.
 * @param userId Id of current user. May by null.
 */
internal class LiveUpdates(
    private val requests: RequestFactory,
    private val preferencesManager: Preferences,
    private var userId: String?,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private var lastActiveTime: Long = preferencesManager.getLastActiveTime()
    private var activeScreenCount = 0
    private var lastCommentId = 0

    // notified in UI thread
    private val dataSubscribers = mutableSetOf<LiveUpdateSubscriber>()
    private var newReplySubscriber: NewReplySubscriber? = null
    private val ticketCountChangedSubscribers = mutableSetOf<OnUnreadTicketCountChangedSubscriber>()

    private var recentUnreadCounter = -1

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStarted = false
    private var firstReplyIsShown = false

    private val ticketsUpdateRunnable = object : Runnable {
        override fun run() {
            val requestUserId = userId
            GlobalScope.launch(ioDispatcher) {
                requests.getTicketsRequest().execute(
                    object : ResponseCallback<List<TicketShortDescription>> {
                        override fun onSuccess(data: List<TicketShortDescription>) {
                            val newUnread = data.count { !it.isRead }
                            this@launch.launch(mainDispatcher) {
                                val userId = PyrusServiceDesk.get().userId
                                if (data.isEmpty())
                                    stopUpdates()
                                else if (requestUserId == userId)
                                    processGetTicketsSuccess(data, newUnread)
                            }
                        }

                        override fun onFailure(responseError: ResponseError) {
                            PLog.d(TAG, "ticketsUpdateRunnable, onFailure")
                        }
                    }
                )
            }
            val interval = getTicketsUpdateInterval(lastActiveTime)
            PLog.d(TAG, "ticketsUpdateRunnable, interval: $interval")
            if (interval == -1L) {
                stopUpdates()
                return
            }
            mainHandler.postDelayed(this, interval)
        }
    }

    /**
     * Registers [subscriber] to on new reply events
     */
    @MainThread
    fun subscribeOnReply(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "subscribeOnReply")
        newReplySubscriber = subscriber
        onSubscribe()
    }

    /**
     * Unregisters [subscriber] from new reply events
     */
    @MainThread
    fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "unsubscribeFromReplies")
        newReplySubscriber = null
        onUnsubscribe()
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
        this.userId = userId
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

        if (newReplySubscriber == null
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

    private fun startUpdates() {
        PLog.d(TAG, "startUpdates")
        isStarted = true
        mainHandler.post(ticketsUpdateRunnable)
    }

    private fun stopUpdates() {
        PLog.d(TAG, "stopUpdates")
        isStarted = false
        mainHandler.removeCallbacks(ticketsUpdateRunnable)
    }

    @MainThread
    private fun processGetTicketsSuccess(data: List<TicketShortDescription>, newUnreadCount: Int) {
        val isChanged = recentUnreadCounter != newUnreadCount
        PLog.d(TAG, "processSuccess, isChanged: $isChanged, recentUnreadCounter: $recentUnreadCounter, newUnreadCount: $newUnreadCount")
        notifyDataSubscribers(data, isChanged, newUnreadCount)
        notifyUnreadCountSubscribers(isChanged, newUnreadCount)
        recentUnreadCounter = newUnreadCount

        val lastSavedComment = preferencesManager.getLastComment()
        val hasUnreadTickets = newUnreadCount > 0

        val lastCommentId = data.firstOrNull()?.lastComment?.commentId ?: return


        if (lastSavedComment != null && lastSavedComment.id == lastCommentId && !lastSavedComment.isRead && !hasUnreadTickets) {
            val chatIsShown = activeScreenCount > 0

            val lastComment = lastSavedComment.copy(isShown = newReplySubscriber != null || chatIsShown, isRead = true)
            preferencesManager.saveLastComment(lastComment)
            if (!chatIsShown)
                notifyNewReplySubscriber(lastComment)
        }
        else if (!hasUnreadTickets && !firstReplyIsShown) {
            if (activeScreenCount <= 0)
                notifyNewReplySubscriber(LastComment(0, true, false, null, null, 0, 0))
        }
        else if (lastSavedComment?.id ?: 0 < lastCommentId && this.lastCommentId < lastCommentId) {
            this.lastCommentId = lastCommentId

            val responseUserId = userId
            PLog.d(TAG, "getTicketRequest")
            GlobalScope.launch(ioDispatcher) {
                val response = requests.getFeedRequest(true).execute()

                launch(mainDispatcher) main@ {
                    if (responseUserId != userId)
                        return@main

                    if (response.hasError()) {
                        this@LiveUpdates.lastCommentId = 0
                        PLog.d(TAG, "response.hasError, error: ${response.getError()}")
                        return@main
                    }

                    val comments = response.getData()?.comments ?: return@main

                    val lastSavedCommentInMainScope = this@LiveUpdates.preferencesManager.getLastComment()
                    val lastServerComment = comments.findLast { !it.isInbound } ?: return@main
                    if (lastServerComment.commentId <= lastSavedCommentInMainScope?.id ?: 0)
                        return@main

                    val lastUserComment = comments.findLast { it.isInbound } ?: return@main

                    updateGetTicketsIntervalIfNeeded(lastUserComment.creationDate.time)

                    val chatIsShown = activeScreenCount > 0
                    val lastComment = LastComment.mapFromComment(
                        newReplySubscriber != null || chatIsShown,
                        !hasUnreadTickets,
                        lastServerComment
                    )

                    this@LiveUpdates.preferencesManager.saveLastComment(lastComment)
                    if (!chatIsShown)
                        notifyNewReplySubscriber(lastComment)
                }
            }
        }
    }

    private fun notifyUnreadCountSubscribers(isChanged: Boolean, newUnreadCount: Int) {
        if (isChanged) {
            val hasNewComments = newUnreadCount > 0
            PLog.d(TAG, "notifyUnreadCountSubscribers, hasNewComments: $hasNewComments, activeScreenCount: $activeScreenCount")
            ticketCountChangedSubscribers.forEach { it.onUnreadTicketCountChanged(newUnreadCount) }
        }
    }

    private fun notifyDataSubscribers(data: List<TicketShortDescription>, isChanged: Boolean, newUnreadCount: Int) {
        dataSubscribers.forEach {
            it.onNewData(data)
            if (isChanged)
                it.onUnreadTicketCountChanged(newUnreadCount)
        }
    }

    private fun notifyNewReplySubscriber(lastComment: LastComment) {
        PLog.d(TAG, "notifyNewReplySubscriber, comment: $lastComment")
        val subscriber = newReplySubscriber ?: return
        firstReplyIsShown = true
        val hasNewComments = !lastComment.isRead
        subscriber.onNewReply(
            hasNewComments,
            if (hasNewComments) lastComment.text else null,
            if (hasNewComments) lastComment.attachesCount else 0,
            if (hasNewComments) lastComment.attaches else null,
            if (hasNewComments) lastComment.utcTime else 0
        )
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
    fun onNewData(tickets: List<TicketShortDescription>)
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