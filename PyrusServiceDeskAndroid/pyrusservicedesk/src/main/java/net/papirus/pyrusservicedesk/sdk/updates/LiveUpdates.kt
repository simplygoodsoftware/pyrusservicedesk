package net.papirus.pyrusservicedesk.sdk.updates

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.utils.MILLISECONDS_IN_MINUTE

private const val TICKETS_UPDATE_INTERVAL = 5L

/**
 * Class for recurring requesting data.
 * Exposes notifications of unread ticket count changes, of new reply from support received.
 * Also exposes result of requesting data.
 *
 * Subscription types: [LiveUpdateSubscriber], [NewReplySubscriber], [OnUnreadTicketCountChangedSubscriber]
 */
internal class LiveUpdates(requests: RequestFactory) {

    // notified in UI thread
    private val dataSubscribers = mutableSetOf<LiveUpdateSubscriber>()
    private val newReplySubscribers = mutableSetOf<NewReplySubscriber>()
    private val ticketCountChangedSubscribers = mutableSetOf<OnUnreadTicketCountChangedSubscriber>()

    private var recentUnreadCounter = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ticketsUpdateRunnable = object : Runnable {
        override fun run() {
            GlobalScope.launch(Dispatchers.IO) {
                requests.getTicketsRequest().execute(
                    object: ResponseCallback<List<TicketShortDescription>> {
                        override fun onSuccess(data: List<TicketShortDescription>) {
                            val newUnread = data.count{ !it.isRead }
                            this@launch.launch(Dispatchers.Main) {
                                processSuccess(data, newUnread)
                            }
                        }
                        override fun onFailure(responseError: ResponseError) {
                        }
                    }
                )
            }
            mainHandler.postDelayed(this, TICKETS_UPDATE_INTERVAL * MILLISECONDS_IN_MINUTE)
        }
    }

    init {
        mainHandler.post(ticketsUpdateRunnable)
    }

    /**
     * Registers [subscriber] to on new reply events
     */
    fun subscribeOnReply(subscriber: NewReplySubscriber) {
        newReplySubscribers.add(subscriber)
    }

    /**
     * Unregisters [subscriber] from new reply events
     */
    fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
        newReplySubscribers.remove(subscriber)
    }

    /**
     * Registers [subscriber] on unread ticket count changed events
     */
    internal fun subscribeOnUnreadTicketCountChanged(subscriber: OnUnreadTicketCountChangedSubscriber) {
        ticketCountChangedSubscribers.add(subscriber)
    }

    /**
     * Unregisters [subscriber] from unread ticket count changed events
     */
    internal fun unsubscribeFromTicketCountChanged(subscriber: OnUnreadTicketCountChangedSubscriber) {
        ticketCountChangedSubscribers.remove(subscriber)
    }

    /**
     * Registers [liveUpdateSubscriber] on new data received event
     */
    internal fun subscribeOnData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.add(liveUpdateSubscriber)
    }

    /**
     * Unregisters [liveUpdateSubscriber] from new data received event
     */
    internal fun unsubscribeFromData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.remove(liveUpdateSubscriber)
    }

    private fun processSuccess(data: List<TicketShortDescription>, newUnreadCount: Int) {
        val isChanged = recentUnreadCounter != newUnreadCount
        dataSubscribers.forEach{
            it.onNewData(data)
            if (isChanged)
                it.onUnreadTicketCountChanged(newUnreadCount)
        }
        if (isChanged) {
            ticketCountChangedSubscribers.forEach { it.onUnreadTicketCountChanged(newUnreadCount) }
            if (isChanged && newUnreadCount > 0)
                newReplySubscribers.forEach { it.onNewReply() }
        }
        recentUnreadCounter = newUnreadCount
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