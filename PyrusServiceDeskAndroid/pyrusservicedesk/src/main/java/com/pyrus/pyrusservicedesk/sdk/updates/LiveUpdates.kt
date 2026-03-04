package com.pyrus.pyrusservicedesk.sdk.updates

import android.util.Log
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_HOUR
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentInfo
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommentWithAttachmentsEntity
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
            val lastTicket = localTicketsStore.getTickets().lastOrNull()
            val lastMyComment = getLastMyComment(lastTicket, localTicketsStore)
            if (lastTicket != null && lastTicket.isRead != true) {
                notifyNewReplySubscriber(subscriber, lastTicket, lastMyComment)
            }
        }
        startUpdates(injector().preferencesManager)
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
        val updatingIsStarted = isStarted.value
        updateIsStarted(true)
        val localTicketsStore = injector().localTicketsStore
        val repository = injector().repository
        val lastActiveTime = preferencesManager?.getLastActiveTime() ?: -1L
        replayJob = coreScope.launch(Dispatchers.IO) {
            if (getTicketsUpdateInterval(lastActiveTime) == -1L && !updatingIsStarted)
                repository.sync()
            localTicketsStore.getTicketsFlow().collect { tickets ->
                val lastTicket = tickets.lastOrNull()
                val lastMyComment = getLastMyComment(lastTicket, localTicketsStore)
                notifyNewReplySubscribers(lastTicket, lastMyComment)
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

    fun notifyNewReplySubscribers(lastTicket: TicketEntity?, lastMyComment: CommentWithAttachmentsEntity?) {
        newReplySubscribers.forEach {
            coreScope.launch(Dispatchers.IO) {
                val needToNotify =
                    (lastCommentId != lastTicket?.lastComment?.commentId && lastTicket?.isRead == false)
                        || lastTicket?.isRead != replyIsShown
                if (lastTicket != null && needToNotify) {
                    notifyNewReplySubscriber(it, lastTicket, lastMyComment)
                }
            }
        }
    }

    private fun notifyNewReplySubscriber(subscriber: NewReplySubscriber, lastTicket: TicketEntity, lastMyComment: CommentWithAttachmentsEntity?) {
        PLog.d(TAG, "notifyNewReplySubscriber, comment: $lastTicket")
        replyIsShown = lastTicket.isRead == true
        lastCommentId = lastTicket.lastComment?.commentId
        val hasNewComments = hasNewComments(lastTicket, lastMyComment)
        coreScope.launch(Dispatchers.Main) {
            subscriber.onNewReply(
                hasNewComments,
                getLastCommentText(hasNewComments, lastTicket),
                getLastCommentAttachmentsCount(hasNewComments, lastTicket),
                getLastCommentAttachments(hasNewComments,lastTicket),
                getUtcTime(hasNewComments, lastTicket)
            )
        }
    }

    private fun getLastCommentText(hasNewComments: Boolean, lastTicket: TicketEntity) =
        if (hasNewComments) lastTicket.lastComment?.body else null

    private fun getLastCommentAttachmentsCount(hasNewComments: Boolean, lastTicket: TicketEntity) =
        if (hasNewComments && lastTicket.lastComment?.lastAttachmentName != null) 1 else 0

    private fun getLastCommentAttachments(hasNewComments: Boolean, lastTicket: TicketEntity) =
        if (hasNewComments && lastTicket.lastComment?.lastAttachmentName != null)
            listOf(lastTicket.lastComment.lastAttachmentName)
        else null

    private fun getUtcTime(hasNewComments: Boolean, lastTicket: TicketEntity) =
        if (hasNewComments && lastTicket.lastComment?.creationDate != null) lastTicket.lastComment.creationDate else 0

    private fun getLastMyComment(
        lastTicket: TicketEntity?,
        localTicketsStore: LocalTicketsStore,
    ): CommentWithAttachmentsEntity? {
        return lastTicket?.ticketId?.let {
            localTicketsStore.getTicketWithComments(it)?.comments
                ?.filter { comment -> comment.comment.isInbound }
                ?.maxByOrNull { comment -> comment.comment.creationDate }
        }
    }

    private fun hasNewComments(lastTicket: TicketEntity, lastMyComment: CommentWithAttachmentsEntity?): Boolean {
        val commentId = lastTicket.lastComment?.commentId
        val lastMyCommentId = lastMyComment?.comment?.commentId
        return lastTicket.isRead != true
            && commentId != null
            && lastMyCommentId != null
            && lastMyCommentId < commentId
            && commentId > 0
            && !lastTicket.lastComment.isEmptyComment()

    }

    fun CommentInfo.isEmptyComment(): Boolean {
        return this.body.isNullOrBlank() && this.lastAttachmentName.isNullOrBlank()
    }

    companion object {
        private const val TAG = "LiveUpdates"
    }

}