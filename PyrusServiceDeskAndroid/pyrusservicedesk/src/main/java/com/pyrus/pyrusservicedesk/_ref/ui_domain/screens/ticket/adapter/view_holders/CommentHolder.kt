package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.hasError
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.utils.getTimeText
import com.pyrus.pyrusservicedesk.utils.isImage

internal abstract class CommentHolder(
    parent: ViewGroup,
    @LayoutRes layoutRes: Int,
    private val onErrorCommentEntryClickListener: (entry: CommentEntry) -> Unit,
    private val onFileReadyToPreviewClickListener: (attachment: Attachment) -> Unit,
    private val onTextCommentLongClicked: (String) -> Unit,
) : ViewHolderBase<CommentEntry>(parent, layoutRes) {

    abstract val comment: CommentView

    private val onCommentClickListener = View.OnClickListener {
        when {
            getItem().hasError() -> onErrorCommentEntryClickListener.invoke(getItem())
            (comment.contentType == ContentType.Attachment
                || comment.contentType == ContentType.PreviewableAttachment)
                && comment.fileProgressStatus == Status.Completed -> {

                onFileReadyToPreviewClickListener.invoke(getItem().comment.attachments!!.first())
            }
        }
    }

    private val onCommentLongClickListener = View.OnLongClickListener {
        return@OnLongClickListener when {
            !getItem().comment.hasAttachments() -> {
                onTextCommentLongClicked.invoke(getItem().comment.body ?: "")
                true
            }

            else -> false
        }
    }

    override fun bindItem(item: CommentEntry) {
        super.bindItem(item)
        itemView.setOnClickListener {
            if (getItem().hasError()) {
                onErrorCommentEntryClickListener.invoke(getItem())
            }
        }
        comment.setOnLongClickListener(onCommentLongClickListener)
        comment.setOnClickListener(onCommentClickListener)
        comment.status = when {
            getItem().hasError() -> Status.Error
            getItem().comment.isLocal() -> Status.Processing
            else -> Status.Completed
        }
        comment.contentType = when {
            !item.comment.hasAttachments() -> ContentType.Text
            item.comment.attachments!!.first().name.isImage() -> ContentType.PreviewableAttachment
            else -> ContentType.Attachment
        }
        when (comment.contentType){
            ContentType.Text -> bindTextView()
            else -> bindAttachmentView()
        }

        val creationTime =
            if (getItem().comment.isWelcomeMessage) ""
            else getItem().comment.creationDate.getTimeText(itemView.context)

        comment.setCreationTime(creationTime)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        getItem().uploadFileHooks?.unsubscribeFromProgress()
    }

    private fun bindTextView() {
        val text = getItem().comment.body ?: ""
        comment.setCommentText(text)
    }

    private fun bindAttachmentView() {
        getItem().comment.attachments!!.first().let { attachment ->
            comment.setFileName(getItem().comment.attachments?.first()?.name ?: "")
            comment.setFileSize(getItem().comment.attachments?.first()?.bytesSize?.toFloat() ?: 0f)
            comment.setPreview(attachment.getPreviewUrl())
            comment.fileProgressStatus = if (getItem().hasError()) Status.Error else Status.Completed
            comment.setOnProgressIconClickListener {
                when (comment.fileProgressStatus) {
                    Status.Processing -> getItem().uploadFileHooks?.cancelUploading()
                    Status.Completed -> onFileReadyToPreviewClickListener.invoke(getItem().comment.attachments!![0])
                    Status.Error -> onCommentClickListener.onClick(comment)
                }
            }
            if (!getItem().hasError()) {
                getItem().uploadFileHooks?.subscribeOnProgress {
                    comment.setProgress(it)
                    when {
                        it == itemView.resources.getInteger(R.integer.psd_progress_max_value) ->
                            comment.fileProgressStatus = Status.Completed
                        comment.fileProgressStatus != Status.Processing ->
                            comment.fileProgressStatus = Status.Processing
                    }
                }
            }
        }
    }
}