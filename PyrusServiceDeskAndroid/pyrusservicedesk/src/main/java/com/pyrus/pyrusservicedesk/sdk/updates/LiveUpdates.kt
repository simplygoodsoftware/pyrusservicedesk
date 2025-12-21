package com.pyrus.pyrusservicedesk.sdk.updates

import android.util.Log
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
internal class LiveUpdates() {

    private val coreScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        Log.e(TAG, "coreScope global error: ${throwable.message}")
        PLog.e(TAG, "coreScope global error: ${throwable.message}")
        throwable.printStackTrace()
    })
    private var lastCommentId: Long? = null

    // notified in UI thread
    private val newReplySubscribers = mutableSetOf<NewReplySubscriber>()
    var isStarted = false
    private var replyIsShown = false
    private var replayJob: Job? = null


    /**
     * Registers [subscriber] to on new reply events
     */

    @MainThread
    fun subscribeOnReply(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "subscribeOnReply")
        newReplySubscribers.add(subscriber)
        val localTicketsStore = injector().localTicketsStore
        coreScope.launch(Dispatchers.IO) {
            val lastComment = localTicketsStore.getTickets().lastOrNull()
            if (lastComment != null && lastComment.isRead != true) {
                notifyNewReplySubscriber(subscriber, lastComment)
            }
        }
        startUpdates()
    }

    /**
     * Unregisters [subscriber] from new reply events
     */
    @MainThread
    fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
        PLog.d(TAG, "unsubscribeFromReplies")
        newReplySubscribers.remove(subscriber)
        stopUpdates()
    }

    internal fun reset() {
        PLog.d(TAG, "reset")
        lastCommentId = null
        replayJob?.cancel()
        replayJob = null
        if (isStarted)
            startUpdates()
    }

    @MainThread
    private fun startUpdates() {
        PLog.d(TAG, "startUpdates")
        isStarted = true
        val localTicketsStore = injector().localTicketsStore
        replayJob = coreScope.launch(Dispatchers.IO) {
            localTicketsStore.getTicketsFlow().collect { tickets ->
                notifyNewReplySubscribers(tickets.lastOrNull())
            }
        }
    }

    private fun stopUpdates() {
        PLog.d(TAG, "stopUpdates")
        isStarted = false
        replayJob?.cancel()
    }

    fun notifyNewReplySubscribers(lastTicket: TicketEntity?) {
        newReplySubscribers.forEach {
            coreScope.launch(Dispatchers.IO) {
                val needRoNotify =
                    (lastCommentId != lastTicket?.lastComment?.commentId && lastTicket?.isRead == false)
                        || lastTicket?.isRead != replyIsShown
                if (lastTicket != null && needRoNotify) {
                    notifyNewReplySubscriber(it, lastTicket)
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