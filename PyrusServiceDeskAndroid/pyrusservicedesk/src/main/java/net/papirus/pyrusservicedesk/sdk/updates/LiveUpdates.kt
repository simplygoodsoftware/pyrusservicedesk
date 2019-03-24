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

internal class LiveUpdates(requests: RequestFactory) {

    // notified in UI thread
    private val dataSubscribers = mutableSetOf<LiveUpdateSubscriber>()
    private val newReplySubscribers = mutableSetOf<OnNewReplySubscriber>()
    private val ticketCountChangedSubscribers = mutableSetOf<OnUnreadTicketCountChangedSubscriber>()

    private var recentUnreadCounter = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ticketsUpdateRunnable = object : Runnable {
        override fun run() {
            GlobalScope.launch(Dispatchers.IO) {
                requests.getTicketsRequest().execute(
                    object: ResponseCallback<List<TicketShortDescription>> {
                        override fun onSuccess(data: List<TicketShortDescription>) {
                            this@launch.launch(Dispatchers.Main) {
                                val newUnread = data.count{ !it.isRead }
                                val isChanged = recentUnreadCounter != newUnread
                                dataSubscribers.forEach{
                                    it.onNewData(data)
                                    if (isChanged)
                                        it.onUnreadTicketCountChanged(newUnread)
                                }
                                if (isChanged) {
                                    ticketCountChangedSubscribers.forEach { it.onUnreadTicketCountChanged(newUnread) }
                                    if (isChanged && newUnread > 0)
                                        newReplySubscribers.forEach { it.onNewReply() }
                                }
                                recentUnreadCounter = newUnread
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

    fun subscribeOnReply(subscriber: OnNewReplySubscriber) {
        newReplySubscribers.add(subscriber)
    }

    fun unsubscribeFromReplies(subscriber: OnNewReplySubscriber) {
        newReplySubscribers.remove(subscriber)
    }

    internal fun subscribeOnUnreadTicketCountChanged(subscriber: OnUnreadTicketCountChangedSubscriber) {
        ticketCountChangedSubscribers.add(subscriber)
    }

    internal fun unsubscribeFromTicketCountChanged(subscriber: OnUnreadTicketCountChangedSubscriber) {
        ticketCountChangedSubscribers.remove(subscriber)
    }

    internal fun subscribeOnData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.add(liveUpdateSubscriber)
    }

    internal fun unsubscribeFromData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.remove(liveUpdateSubscriber)
    }
}

internal interface LiveUpdateSubscriber: OnUnreadTicketCountChangedSubscriber {
    fun onNewData(tickets: List<TicketShortDescription>)
}

internal interface OnUnreadTicketCountChangedSubscriber{
    fun onUnreadTicketCountChanged(unreadTicketCount: Int)
}