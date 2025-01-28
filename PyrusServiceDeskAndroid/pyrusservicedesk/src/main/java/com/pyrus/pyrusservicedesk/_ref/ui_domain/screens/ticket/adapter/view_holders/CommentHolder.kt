package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2.CommentContent
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase

internal abstract class CommentHolder(
    parent: ViewGroup,
    @LayoutRes layoutRes: Int,
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ViewHolderBase<CommentEntryV2.Comment>(parent, layoutRes) {

    abstract val comment: CommentView

    private val onCommentClickListener = View.OnClickListener {
        when {
            getItem().hasError -> onEvent(TicketView.Event.OnErrorCommentClick(getItem().id))
            (comment.contentType == ContentType.Attachment
                || comment.contentType == ContentType.PreviewableAttachment)
                && comment.fileProgressStatus == Status.Completed -> {
                val content = getItem().content
                if (content is CommentContent.Image) {
                    onEvent(TicketView.Event.OnPreviewClick(getItem().id, content.attachId))
                }

            }
        }
    }

    private val onCommentLongClickListener = View.OnLongClickListener {
        val content = getItem().content
        return@OnLongClickListener when {
            content is CommentContent.Text -> {
                onEvent(TicketView.Event.OnCopyClick(content.text))
                true
            }
            else -> false
        }
    }

    override fun bindItem(entry: CommentEntryV2.Comment) {
        super.bindItem(entry)

        comment.setOnLongClickListener(onCommentLongClickListener)
        comment.setOnClickListener(onCommentClickListener)
        comment.status = entry.status
        comment.contentType = entry.contentType

        comment.setCreationTime(entry.timeText?.text(comment.context))

        itemView.setOnClickListener {
            if (entry.hasError) {
                onEvent.invoke(TicketView.Event.OnErrorCommentClick(getItem().id))
            }
        }

        when (val content = entry.content) {
            is CommentContent.Image -> bindAttachmentView(content)
            is CommentContent.Text -> bindTextView(content)
        }
    }

    private fun bindTextView(content: CommentContent.Text) {
        comment.setCommentText(content.text)
    }

    private fun bindAttachmentView(content: CommentContent.Image) {
        comment.setFileName(content.attachmentName)
        comment.setFileSize(content.fileSize)
        comment.setPreview(content.attachUrl)
        comment.fileProgressStatus = content.fileProgressStatus
        comment.setProgress(content.uploadProgress ?: 0)
        comment.setOnProgressIconClickListener {
            val imageContent = getItem().content as? CommentContent.Image ?: return@setOnProgressIconClickListener
            onEvent(TicketView.Event.OnCancelUploadClick(getItem().id, imageContent.attachId))
        }
    }
}