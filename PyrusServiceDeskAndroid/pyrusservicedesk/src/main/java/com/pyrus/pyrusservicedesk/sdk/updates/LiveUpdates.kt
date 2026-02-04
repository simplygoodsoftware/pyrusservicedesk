package com.pyrus.pyrusservicedesk.sdk.updates

import android.util.Log
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.refresh
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_HOUR
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Class for recurring requesting data.
 * Exposes notifications of unread ticket count changes, of new reply from support received.
 * Also exposes result of requesting data.
 *
 * Subscription types: [NewReplySubscriber]
 *
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
    val isStarted =  MutableStateFlow(false)
    fun isStartedFlow(): StateFlow<Boolean> = isStarted
    private var replyIsShown = false
    private var replayJob: Job? = null

    private fun updateIsStarted(isStarted: Boolean) {
        this.isStarted.value = isStarted
    }


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
        startUpdates(injector().preferencesManager)
        refresh()
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

    internal fun reset(preferencesManager: PreferencesManager?) {
        PLog.d(TAG, "reset")
        lastCommentId = null
        replayJob?.cancel()
        replayJob = null
        if (isStarted.value)
            startUpdates(preferencesManager)
    }

    @MainThread
    private fun startUpdates(preferencesManager: PreferencesManager?) {
        PLog.d(TAG, "startUpdates")
        updateIsStarted(true)
        val localTicketsStore = injector().localTicketsStore
        val repository = injector().repository
        val lastActiveTime = preferencesManager?.getLastActiveTime() ?: -1L
        replayJob = coreScope.launch(Dispatchers.IO) {
            if (getTicketsUpdateInterval(lastActiveTime) != -1L)
                repository.sync()
            localTicketsStore.getTicketsFlow().collect { tickets ->
                notifyNewReplySubscribers(tickets.lastOrNull())
            }
        }
    }

    fun getTicketsUpdateInterval(lastActiveTime: Long): Long {
        val diff = System.currentTimeMillis() - lastActiveTime
        return when {
            diff <= MILLISECONDS_IN_MINUTE -> 5L * MILLISECONDS_IN_SECOND
            diff <= 5 * MILLISECONDS_IN_MINUTE -> 15L * MILLISECONDS_IN_SECOND
            diff <= MILLISECONDS_IN_HOUR -> MILLISECONDS_IN_MINUTE.toLong()
            diff <= 3 * MILLISECONDS_IN_DAY || PyrusServiceDesk.sdIsOpen.value -> 3 * MILLISECONDS_IN_MINUTE.toLong()
            else -> -1L
        }
    }

    private fun stopUpdates() {
        PLog.d(TAG, "stopUpdates")
        updateIsStarted(false)
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