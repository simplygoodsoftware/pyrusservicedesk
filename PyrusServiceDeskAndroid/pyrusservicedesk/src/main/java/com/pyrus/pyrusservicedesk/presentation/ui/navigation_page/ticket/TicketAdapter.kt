package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.graphics.Canvas
import android.net.Uri
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_SWIPE
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.*
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceMultiplier
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getAvatarUrl
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getPreviewUrl
import com.pyrus.pyrusservicedesk.utils.getTimeText
import com.pyrus.pyrusservicedesk.utils.isImage
import com.squareup.picasso.Picasso
import kotlin.math.abs


private const val VIEW_TYPE_COMMENT_INBOUND = 0
private const val VIEW_TYPE_COMMENT_OUTBOUND = 1
private const val VIEW_TYPE_WELCOME_MESSAGE = 2
private const val VIEW_TYPE_DATE = 3

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketAdapter: AdapterBase<TicketEntry>() {

    override val itemTouchHelper: ItemTouchHelper? = ItemTouchHelper(TouchCallback())

    /**
     * [SpaceMultiplier] implementation for customizing spaces between items fot the feed.
     */
    val itemSpaceMultiplier = object: SpaceMultiplier{
        override fun getMultiplier(adapterPosition: Int): Float {
            return when {
                adapterPosition <= 0 -> 1f
                itemsList[adapterPosition].type == Type.Comment
                        && itemsList[adapterPosition -1].type == Type.Comment
                        && (itemsList[adapterPosition] as CommentEntry).comment.isInbound !=
                            (itemsList[adapterPosition - 1] as CommentEntry).comment.isInbound -> 2f
                else -> 1f
            }
        }
    }
    private var onFileReadyToPreviewClickListener: ((Attachment) -> Unit)? = null
    private var onTextCommentLongClicked: ((String) -> Unit)? = null
    private var recentInboundCommentPositionWithAvatar = 0

    override fun getItemViewType(position: Int): Int {
        return with(itemsList[position]) {
            return@with when {
                type == Type.Date -> VIEW_TYPE_DATE
                type == Type.WelcomeMessage -> VIEW_TYPE_WELCOME_MESSAGE
                (this as CommentEntry).comment.isInbound -> VIEW_TYPE_COMMENT_OUTBOUND
                else -> VIEW_TYPE_COMMENT_INBOUND
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<TicketEntry> {
        return when(viewType){
            VIEW_TYPE_COMMENT_INBOUND -> InboundCommentHolder(parent)
            VIEW_TYPE_COMMENT_OUTBOUND -> OutboundCommentHolder(parent)
            VIEW_TYPE_WELCOME_MESSAGE -> WelcomeMessageHolder(parent)
            else -> DateViewHolder(parent)
        } as ViewHolderBase<TicketEntry>
    }

    override fun onViewAttachedToWindow(holder: ViewHolderBase<TicketEntry>) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.translationX = 0f
        holder.itemView.findViewById<View>(R.id.author_and_comment)?.let { it.translationX = 0f }
    }

    override fun setItems(items: List<TicketEntry>) {
        this.itemsList = items.toMutableList()
    }

    /**
     * Assigns [listener] ths is invoked when comment with the file that is
     * ready to be previewed was clicked.
     */
    fun setOnFileReadyForPreviewClickListener(listener: (attachment: Attachment) -> Unit) {
        onFileReadyToPreviewClickListener = listener
    }

    /**
     * Assigns [listener] that is invoked when text comment was long pressed by the user.
     */
    fun setOnTextCommentLongClicked(listener: (String) -> Unit) {
        onTextCommentLongClicked = listener
    }

    private inner class InboundCommentHolder(parent: ViewGroup) :
        CommentHolder(parent, R.layout.psd_view_holder_comment_inbound) {

        override val comment: CommentView = itemView.findViewById(R.id.comment)
        override val creationTime: TextView = itemView.findViewById(R.id.creation_time)
        private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        private val authorName = itemView.findViewById<TextView>(R.id.author_name)

        override fun bindItem(item: CommentEntry) {
            super.bindItem(item)
            setAuthorNameVisibility(shouldShowAuthorName())
            with(shouldShowAuthorAvatar()) {
                setAuthorAvatarVisibility(this)
                if (this && shouldRedrawRecentCommentWithAvatar()) {
                    val toRedraw = recentInboundCommentPositionWithAvatar
                    itemView.post { notifyItemChanged(toRedraw) }
                    recentInboundCommentPositionWithAvatar = adapterPosition
                }
            }
        }

        private fun shouldRedrawRecentCommentWithAvatar(): Boolean =
            adapterPosition == itemsList.lastIndex && recentInboundCommentPositionWithAvatar != adapterPosition

        private fun setAuthorNameVisibility(visible: Boolean) {
            authorName.visibility = if (visible) VISIBLE else GONE
            if (visible) {
            }
            authorName.text = getItem().comment.author.name
        }

        private fun setAuthorAvatarVisibility(visible: Boolean) {
            avatar.visibility = if (visible) VISIBLE else INVISIBLE
            if (visible) {
                Picasso.get()
                    .load(getAvatarUrl(getItem().comment.author.avatarId))
                    .placeholder(ConfigUtils.getSupportAvatar(itemView.context))
                    .transform(CIRCLE_TRANSFORMATION)
                    .into(avatar)
            }
        }

        private fun shouldShowAuthorName(): Boolean {
            return adapterPosition == 0
                    || with(itemsList[adapterPosition - 1]) {
                when {
                    this.type != Type.Comment -> true
                    else -> getItem().comment.author != (this as CommentEntry).comment.author
                }
            }
        }

        private fun shouldShowAuthorAvatar(): Boolean {
            return with(itemsList.getOrNull(adapterPosition + 1)) {
                when {
                    this?.type != Type.Comment -> true
                    else -> getItem().comment.author != (this as CommentEntry).comment.author
                }
            }
        }
    }

    private inner class OutboundCommentHolder(parent: ViewGroup)
        : CommentHolder(parent, R.layout.psd_view_holder_comment_outbound){

        override val comment: CommentView = itemView.findViewById(R.id.comment)
        override val creationTime: TextView = itemView.findViewById(R.id.creation_time)
    }

    private abstract inner class CommentHolder(
            parent: ViewGroup,
            @LayoutRes layoutRes: Int)
        : ViewHolderBase<CommentEntry>(parent, layoutRes){

        abstract val comment: CommentView
        abstract val creationTime: TextView

        val onCommentClickListener = OnClickListener {
            when {
                (comment.contentType == ContentType.Attachment
                        || comment.contentType == ContentType.AttachmentFullSize)
                        && comment.fileProgressStatus == Status.Completed -> {

                    onFileReadyToPreviewClickListener?.invoke(getItem().comment.attachments!!.first())
                }

                else -> getItem().onClickedCallback.onClicked(getItem())
            }
        }

        val onCommentLongClickListener = OnLongClickListener {
            return@OnLongClickListener when {
                !getItem().comment.hasAttachments() -> {
                    onTextCommentLongClicked?.invoke(getItem().comment.body)
                    true
                }
                else -> false
            }
        }

        override fun bindItem(item: CommentEntry) {
            super.bindItem(item)
            comment.setOnLongClickListener(onCommentLongClickListener)
            comment.setOnClickListener(onCommentClickListener)
            comment.status = when {
                getItem().hasError() -> Status.Error
                getItem().comment.isLocal() -> Status.Processing
                else -> Status.Completed
            }
            comment.contentType = when {
                !item.comment.hasAttachments() -> ContentType.Text
                item.comment.attachments!!.first().name.isImage() -> ContentType.AttachmentFullSize
                else -> ContentType.Attachment
            }
            when (comment.contentType){
                ContentType.Text -> bindTextView()
                ContentType.Attachment -> bindAttachmentView()
                ContentType.AttachmentFullSize -> bindAttachmentPreview()
            }
            creationTime.text = getItem().comment.creationDate.getTimeText(itemView.context)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            getItem().uploadFileHooks?.unsubscribeFromProgress()
        }

        private fun bindTextView() {
            comment.setCommentText(getItem().comment.body)
        }

        private fun bindAttachmentView() {
            comment.setFileName(getItem().comment.attachments?.first()?.name ?: "")
            comment.setFileSize(getItem().comment.attachments?.first()?.bytesSize?.toFloat() ?: 0f)
            comment.isFileProgressVisible = true
            comment.fileProgressStatus = if (getItem().hasError()) Status.Error else Status.Completed
            comment.setOnProgressIconClickListener {
                when (comment.fileProgressStatus) {
                    Status.Processing -> getItem().uploadFileHooks?.cancelUploading()
                    Status.Completed -> onFileReadyToPreviewClickListener?.invoke(getItem().comment.attachments!![0])
                    Status.Error -> comment.performClick()
                }
            }
            if (!getItem().hasError()) {
                getItem().uploadFileHooks?.subscribeOnProgress {
                    if (comment.fileProgressStatus != Status.Processing)
                        comment.fileProgressStatus = Status.Processing
                    comment.setProgress(it)
                }
            }
        }

        private fun bindAttachmentPreview() {
            getItem().comment.attachments!!.first().let {
                val previewUri = it.uri ?: Uri.parse(getPreviewUrl(it.id))
                comment.setPreview(previewUri,it.uri != null)
            }
        }
    }

    private class WelcomeMessageHolder(parent: ViewGroup) :
        ViewHolderBase<WelcomeMessageEntry>(parent, R.layout.psd_view_holder_comment_inbound) {

        private val comment: CommentView = itemView.findViewById(R.id.comment)
        private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        private val authorName = itemView.findViewById<TextView>(R.id.author_name)

        override fun bindItem(item: WelcomeMessageEntry) {
            super.bindItem(item)
            authorName.visibility = GONE
            avatar.visibility = INVISIBLE
            comment.contentType = ContentType.Text
            comment.setCommentText(item.message)
        }
    }

    private class DateViewHolder(parent: ViewGroup)
        : ViewHolderBase<DateEntry>(parent, R.layout.psd_view_holder_date) {

        private val date = itemView.findViewById<TextView>(R.id.date)

        override fun bindItem(item: DateEntry) {
            super.bindItem(item)
            date.text = item.date
        }
    }

    private inner class TouchCallback : ItemTouchHelper.Callback() {

        override fun getMovementFlags(recyclerView: RecyclerView,
                                      viewHolder: RecyclerView.ViewHolder): Int {

            return makeFlag(ACTION_STATE_SWIPE,  ItemTouchHelper.LEFT)
        }

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        }

        override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
            return Float.MAX_VALUE
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return Float.MAX_VALUE
        }

        override fun onChildDraw(c: Canvas,
                                 recyclerView: RecyclerView,
                                 viewHolder: RecyclerView.ViewHolder,
                                 dX: Float,
                                 dY: Float,
                                 actionState: Int,
                                 isCurrentlyActive: Boolean) {

            if (viewHolder.adapterPosition == -1 || itemsList[viewHolder.adapterPosition].isNonShiftable())
                return
            val maxItemViewShift = recyclerView.resources.getDimensionPixelSize(R.dimen.psd_comment_creation_time_width)
            val minInboundOffset =  recyclerView.resources.getDimensionPixelSize(R.dimen.psd_offset_default)
            var x = dX
            if (x < -maxItemViewShift)
                x = -maxItemViewShift.toFloat()
            for (position in 0..(recyclerView.childCount - 1)) {
                recyclerView.findContainingViewHolder(recyclerView.getChildAt(position))?.let {
                    if (it.adapterPosition == - 1 || itemsList[it.adapterPosition].isNonShiftable())
                        return@let
                    it.itemView.translationX = x
                    if (itemsList[it.adapterPosition].isConsideredInbound()) {
                        it.itemView.findViewById<View>(R.id.author_and_comment)?.let { author_and_comment ->
                            if(abs(x) > author_and_comment.left - minInboundOffset)
                                author_and_comment.translationX = abs(x) - author_and_comment.left + minInboundOffset
                            else
                                author_and_comment.translationX = 0f
                        }
                    }
                }
            }
        }
    }
}

private fun TicketEntry.isConsideredInbound(): Boolean {
    return type == Type.WelcomeMessage
            || type == Type.Comment && !(this as CommentEntry).comment.isInbound
}

private fun TicketEntry.isNonShiftable(): Boolean = type == Type.Date
