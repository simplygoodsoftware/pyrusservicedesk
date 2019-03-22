package net.papirus.pyrusservicedesk.presentation.viewmodel

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.papirus.pyrusservicedesk.UnreadCounterChangedSubscriber
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.utils.MILLISECONDS_IN_MINUTE

private const val TICKETS_UPDATE_INTERVAL = 5L

internal class LiveUpdates(requests: RequestFactory) {

    // notified in UI thread
    private val dataSubscribers = mutableSetOf<LiveUpdateSubscriber>()
    private val counterChangedSubscribers = mutableSetOf<UnreadCounterChangedSubscriber>()

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
                                        it.onUnreadCounterChanged(newUnread)
                                }
                                if (isChanged)
                                    counterChangedSubscribers.forEach{ it.onUnreadCounterChanged(newUnread) }
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

    fun subscribeOnUnreadCounterChanged(subscriber: UnreadCounterChangedSubscriber) {
        counterChangedSubscribers.add(subscriber)
    }

    fun unsubscribeFromUnreadCounterChanged(subscriber: UnreadCounterChangedSubscriber) {
        counterChangedSubscribers.remove(subscriber)
    }

    fun subscribeOnData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.add(liveUpdateSubscriber)
    }

    fun unsubscribeFromData(liveUpdateSubscriber: LiveUpdateSubscriber) {
        dataSubscribers.remove(liveUpdateSubscriber)
    }

    internal interface LiveUpdateSubscriber: UnreadCounterChangedSubscriber {
        fun onNewData(tickets: List<TicketShortDescription>)
    }
}