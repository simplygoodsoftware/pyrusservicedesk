package com.pyrus.pyrusservicedesk.sdk.updates

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Class for recurring requesting data.
 * Exposes notifications of unread ticket count changes, of new reply from support received.
 * Also exposes result of requesting data.
 *
 * Subscription types: [LiveUpdateSubscriber], [NewReplySubscriber], [OnUnreadTicketCountChangedSubscriber]
 *
 * @param requests Service desk request factory.
 * @param preferences App shared preferences.
 * @param userId Id of current user. May by null.
 */
internal class LiveUpdates(requests: RequestFactory, private val preferences: SharedPreferences, private var userId: String?) {

    private var lastActiveTime: Long = preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L)
    private var activeScreenCount = 0

    // notified in UI thread
    private val dataSubscribers = mutableSetOf<LiveUpdateSubscriber>()
    private val newReplySubscribers = mutableSetOf<NewReplySubscriber>()
    private val newReplyLocalSubscribers = mutableSetOf<NewReplySubscriber>()
    private val ticketCountChangedSubscribers = mutableSetOf<OnUnreadTicketCountChangedSubscriber>()

    private var recentUnreadCounter = -1

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStarted = false
    private var hasUnread: Boolean? = null

    private val ticketsUpdateRunnable = object : Runnable {
        override fun run() {
            val requestUserId = userId
            GlobalScope.launch(Dispatchers.IO) {
                requests.getTicketsRequest().execute(
                    object : ResponseCallback<List<TicketShortDescription>> {
                        override fun onSuccess(data: List<TicketShortDescription>) {
                            val newUnread = data.count { !it.isRead }
                            this@launch.launch(Dispatchers.Main) {
                                val userId = PyrusServiceDesk.get().userId
                                if (data.isEmpty())
                                    stopUpdates()
                                else if (requestUserId == userId)
                                    processSuccess(data, newUnread)
                            }
                        }

                        override fun onFailure(responseError: ResponseError) {
                            // ¯\_(ツ)_/¯
                        }
                    }
                )
            }
            val interval = getTicketsUpdateInterval(lastActiveTime)
            if (interval == -1L) {
                stopUpdates()
                return
            }
            mainHandler.postDelayed(this, interval)
        }
    }

    init {
        if (!isStarted)
            startUpdates()
    }

    /**
     * Registers [subscriber] to on new reply events
     */
    @MainThread
    fun subscribeOnReply(subscriber: NewReplySubscriber) {
        newReplySubscribers.add(subscriber)
        hasUnread?.let { subscriber.onNewReply(it) }
        onSubscribe()
    }

    /**
     * Unregisters [subscriber] from new reply events
     */
    @MainThread
    fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
        newReplySubscribers.remove(subscriber)
        onUnsubscribe()
    }

    /**
     * Registers [subscriber] to on new reply events. For local subscribers.
     */
    @MainThread
    fun subscribeOnLocalReply(subscriber: NewReplySubscriber) {
        newReplyLocalSubscribers.add(subscriber)
        hasUnread?.let { subscriber.onNewReply(it) }
        onSubscribe()
    }

    /**
    * Unregisters [subscriber] from new reply events. For local subscribers.
    */
    @MainThread
    fun unsubscribeFromLocalReplies(subscriber: NewReplySubscriber) {
        newReplyLocalSubscribers.remove(subscriber)
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

    /**
     * Start tickets update if it is not already running.
     */
    internal fun startUpdatesIfNeeded(lastActiveTime: Long) {
        val currentLastActiveTime = preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L)
        if (lastActiveTime > currentLastActiveTime)
            preferences.edit().putLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, lastActiveTime).commit()
        val interval = getTicketsUpdateInterval(lastActiveTime)
        val currentInterval = getTicketsUpdateInterval(currentLastActiveTime)

        this.lastActiveTime = lastActiveTime
        if (interval == currentInterval && isStarted)
            return
        if (isStarted)
            stopUpdates()
        if (interval != -1L || activeScreenCount > 0)
            startUpdates()
    }

    /**
     * Reset unread token count to 0.
     */
    internal fun resetUnreadCount() {
        if (hasUnread != null)
            hasUnread = false
        recentUnreadCounter = 0
    }

    /**
     * Increase active screen count by one.
     */
    internal fun increaseActiveScreenCount() {
        activeScreenCount++
    }

    /**
     * Decrease active screen count by one.
     */
    internal fun decreaseActiveScreenCount() {
        activeScreenCount--
        startUpdatesIfNeeded(preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L))
    }

    private fun onSubscribe() {
        startUpdatesIfNeeded(preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L))
    }

    private fun onUnsubscribe() {
        if (!isStarted)
            return

        if (newReplySubscribers.isEmpty()
            && ticketCountChangedSubscribers.isEmpty()
            && dataSubscribers.isEmpty()
            && newReplyLocalSubscribers.isEmpty()
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
        isStarted = true
        mainHandler.post(ticketsUpdateRunnable)
    }

    private fun stopUpdates() {
        isStarted = false
        mainHandler.removeCallbacks(ticketsUpdateRunnable)
    }

    @MainThread
    private fun processSuccess(data: List<TicketShortDescription>, newUnreadCount: Int) {
        val isChanged = recentUnreadCounter != newUnreadCount
        dataSubscribers.forEach {
            it.onNewData(data)
            if (isChanged)
                it.onUnreadTicketCountChanged(newUnreadCount)
        }
        hasUnread = newUnreadCount > 0
        if (isChanged) {
            val hasNewComments = newUnreadCount > 0
            ticketCountChangedSubscribers.forEach { it.onUnreadTicketCountChanged(newUnreadCount) }
            if (activeScreenCount <= 0)
                newReplySubscribers.forEach { it.onNewReply(hasNewComments) }
            newReplyLocalSubscribers.forEach { it.onNewReply(hasNewComments) }
        }
        recentUnreadCounter = newUnreadCount
    }

    fun reset(userId: String?) {
        this.userId = userId
        recentUnreadCounter = -1
        hasUnread = null
        //TODO add active time reset
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