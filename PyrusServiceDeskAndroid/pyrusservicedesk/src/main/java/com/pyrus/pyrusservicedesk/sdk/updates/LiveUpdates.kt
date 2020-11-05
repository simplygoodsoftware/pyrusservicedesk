package com.pyrus.pyrusservicedesk.sdk.updates

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.log.PLog
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.utils.*
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
    private var hasUnread: Boolean = false
    private var lastNotificationIsShown = true

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

    init {
        if (!isStarted)
            startUpdates()
    }

    /**
     * Registers [subscriber] to on new reply events
     */
    @MainThread
    fun subscribeOnReply(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "subscribeOnReply")
        newReplySubscribers.add(subscriber)
        if (!lastNotificationIsShown) {
            lastNotificationIsShown = true
            PLog.d(TAG, "subscribeOnReply, NewReplySubscriber (hasUnreadComments = $hasUnread)")
            subscriber.onNewReply(hasUnread)
        }
        onSubscribe()
    }

    /**
     * Unregisters [subscriber] from new reply events
     */
    @MainThread
    fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "unsubscribeFromReplies")
        newReplySubscribers.remove(subscriber)
        onUnsubscribe()
    }

    /**
     * Registers [subscriber] to on new reply events. For local subscribers.
     */
    @MainThread
    fun subscribeOnLocalReply(subscriber: NewReplySubscriber) {
        newReplyLocalSubscribers.add(subscriber)
        subscriber.onNewReply(hasUnread)
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
        val maxLastActiveTime = max(preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L), lastActiveTime)
        val currentLastActiveTime = preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L)
        PLog.d(TAG, "startUpdatesIfNeeded, lastActiveTime: $lastActiveTime, currentLastActiveTime: $currentLastActiveTime")
        if (maxLastActiveTime > currentLastActiveTime) {
            PLog.d(TAG, "startUpdatesIfNeeded, write last active time in preferences, time: $maxLastActiveTime")
            preferences.edit().putLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, lastActiveTime).commit()
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
     * Reset unread token count to 0.
     */
    internal fun resetUnreadCount() {
        PLog.d(TAG, "resetUnreadCount, hasUnread: $hasUnread, recentUnreadCounter: $recentUnreadCounter")
        hasUnread = false
        recentUnreadCounter = 0
    }

    /**
     * Increase active screen count by one.
     */
    internal fun increaseActiveScreenCount() {
        activeScreenCount++
        PLog.d(TAG, "increaseActiveScreenCount, activeScreenCount: $activeScreenCount")
        startUpdatesIfNeeded(preferences.getLong(PREFERENCE_KEY_LAST_ACTIVITY_TIME, -1L))
    }

    /**
     * Decrease active screen count by one.
     */
    internal fun decreaseActiveScreenCount() {
        activeScreenCount--
        PLog.d(TAG, "decreaseActiveScreenCount, activeScreenCount: $activeScreenCount")
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
    private fun processSuccess(data: List<TicketShortDescription>, newUnreadCount: Int) {
        val isChanged = recentUnreadCounter != newUnreadCount
        PLog.d(TAG, "processSuccess, isChanged: $isChanged, recentUnreadCounter: $recentUnreadCounter, newUnreadCount: $newUnreadCount")
        dataSubscribers.forEach {
            it.onNewData(data)
            if (isChanged)
                it.onUnreadTicketCountChanged(newUnreadCount)
        }
        hasUnread = newUnreadCount > 0
        if (isChanged) {
            val hasNewComments = newUnreadCount > 0
            PLog.d(TAG, "processSuccess, hasNewComments: $hasNewComments, activeScreenCount: $activeScreenCount")
            ticketCountChangedSubscribers.forEach { it.onUnreadTicketCountChanged(newUnreadCount) }
            notifyOnNewReplySubscribers(hasNewComments)
            newReplyLocalSubscribers.forEach { it.onNewReply(hasNewComments) }
        }
        recentUnreadCounter = newUnreadCount
    }

    private fun notifyOnNewReplySubscribers(hasNewComments: Boolean) {
        if (newReplySubscribers.isEmpty()) {
            lastNotificationIsShown = false
            return
        }
        lastNotificationIsShown = true
        if (activeScreenCount > 0)
            return

        PLog.d(TAG, "notifyOnNewReplySubscribers, NewReplySubscriber(hasUnreadComments = $hasNewComments)")
        newReplySubscribers.forEach { it.onNewReply(hasNewComments) }
    }

    fun reset(userId: String?) {
        PLog.d(TAG, "reset")
        this.userId = userId
        recentUnreadCounter = -1
        //TODO add active time reset
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