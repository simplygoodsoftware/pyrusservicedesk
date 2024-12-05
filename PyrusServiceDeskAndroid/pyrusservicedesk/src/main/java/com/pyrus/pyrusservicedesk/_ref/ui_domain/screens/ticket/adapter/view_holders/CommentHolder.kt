package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk._ref.utils.getTimeText

internal abstract class CommentHolder(
    parent: ViewGroup,
    @LayoutRes layoutRes: Int,
    private val onErrorCommentEntryClickListener: (entry: CommentEntryV2.Comment) -> Unit,
    private val onFileReadyToPreviewClickListener: (attachment: Attachment) -> Unit,
    private val onTextCommentLongClicked: (String) -> Unit,
) : ViewHolderBase<CommentEntryV2.Comment>(parent, layoutRes) {

    abstract val comment: CommentView

    private val onCommentClickListener = View.OnClickListener {
        when {
            getItem().hasError -> onErrorCommentEntryClickListener.invoke(getItem())
            (comment.contentType == ContentType.Attachment
                || comment.contentType == ContentType.PreviewableAttachment)
                && comment.fileProgressStatus == Status.Completed -> {
                    TODO()
//                onFileReadyToPreviewClickListener.invoke(getItem().comment.attachments!!.first())
            }
        }
    }

    private val onCommentLongClickListener = View.OnLongClickListener {
        TODO()
//        return@OnLongClickListener when {
//            !getItem().comment.hasAttachments() -> {
//                onTextCommentLongClicked.invoke(getItem().comment.body ?: "")
//                true
//            }
//
//            else -> false
//        }
    }

    override fun bindItem(entry: CommentEntryV2.Comment) {
        super.bindItem(entry)

        comment.setOnLongClickListener(onCommentLongClickListener)
        comment.setOnClickListener(onCommentClickListener)
        comment.status = entry.status
        comment.contentType = entry.contentType

        comment.setCreationTime(entry.timeText)


        itemView.setOnClickListener {
            if (entry.hasError) {
                onErrorCommentEntryClickListener.invoke(entry)
            }
        }

        when (entry.contentType) {
            ContentType.Text -> bindTextView(entry)
            else -> bindAttachmentView(entry)
        }
    }

    private fun bindTextView(entry: CommentEntryV2.Comment) {
        comment.setCommentText(entry.text)
    }

    private fun bindAttachmentView(entry: CommentEntryV2.Comment) {
        val fileUrl = entry.attachUrl ?: return

        comment.setFileName(entry.attachmentName)
        comment.setFileSize(entry.fileSize)
        comment.setPreview(fileUrl)
        comment.fileProgressStatus = entry.fileProgressStatus
        comment.setOnProgressIconClickListener {
            TODO()
        }
    }
}