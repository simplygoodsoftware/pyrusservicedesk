package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.graphics.Canvas
import android.support.annotation.LayoutRes
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_SWIPE
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.pyrusservicedesk.R
import com.squareup.picasso.Picasso
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.DateEntry
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.TicketEntry
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.Type
import net.papirus.pyrusservicedesk.presentation.ui.view.CommentView
import net.papirus.pyrusservicedesk.presentation.ui.view.ContentType
import net.papirus.pyrusservicedesk.presentation.ui.view.Status
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import net.papirus.pyrusservicedesk.sdk.getAvatarUrl
import net.papirus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import net.papirus.pyrusservicedesk.utils.getSimpleAvatar
import net.papirus.pyrusservicedesk.utils.getTimeText


private const val VIEW_TYPE_COMMENT_INBOUND = 0
private const val VIEW_TYPE_COMMENT_OUTBOUND = 1
private const val VIEW_TYPE_DATE = 2

internal class TicketAdapter: AdapterBase<TicketEntry>() {

    override val itemTouchHelper: ItemTouchHelper? = ItemTouchHelper(TouchCallback())
    private var onDownloadedFileClickListener: ((Int) -> Unit)? = null

    override fun getItemViewType(position: Int): Int {
        return with(itemsList[position]) {
            return@with when {
                type == Type.Date -> VIEW_TYPE_DATE
                (this as CommentEntry).comment.isInbound -> VIEW_TYPE_COMMENT_OUTBOUND
                else -> VIEW_TYPE_COMMENT_INBOUND
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<TicketEntry> {
        return when(viewType){
            VIEW_TYPE_COMMENT_INBOUND -> InboundCommentHolder(parent)
            VIEW_TYPE_COMMENT_OUTBOUND -> OutboundCommentHolder(parent)
            else -> DateViewHolder(parent)
        } as ViewHolderBase<TicketEntry>
    }

    override fun onViewAttachedToWindow(holder: ViewHolderBase<TicketEntry>) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.translationX = 0f
    }

    fun setOnDownloadedFileClickListener(listener: (fileId: Int) -> Unit) {
        onDownloadedFileClickListener = listener
    }

    private inner class InboundCommentHolder(parent: ViewGroup)
        : CommentHolder(parent, R.layout.psd_view_holder_comment_inbound){

        override val comment: CommentView = itemView.findViewById(R.id.comment)
        override val creationTime: TextView = itemView.findViewById(R.id.creation_time)
        private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        private val authorName = itemView.findViewById<TextView>(R.id.author_name)

        override fun bindItem(item: CommentEntry) {
            super.bindItem(item)
            avatar.setImageDrawable(
                    ResourcesCompat.getDrawable(
                            itemView.resources,
                            R.drawable.psd_counter_background,
                            null))

            setAuthorNameVisibility(shouldShowAuthorName())
            setAuthorAvatarVisibility(shouldShowAuthorAvatar())
        }

        private fun setAuthorNameVisibility(visible: Boolean) {
            authorName.visibility = if (visible) VISIBLE else GONE
            if (visible)
                authorName.text = getItem().comment.author.name
        }

        private fun setAuthorAvatarVisibility(visible: Boolean) {
            avatar.visibility = if (visible) VISIBLE else INVISIBLE
            if (visible) {
                Picasso.get()
                    .load(getAvatarUrl(getItem().comment.author.avatarId))
                    .placeholder(getSimpleAvatar(itemView.context, getItem().comment.author))
                    .transform(CIRCLE_TRANSFORMATION)
                    .into(avatar)
            }
        }

        private fun shouldShowAuthorName(): Boolean {
            return adapterPosition == 0
                    || with(itemsList[adapterPosition - 1]){
                        return when {
                            this.type != Type.Comment -> true
                            else -> getItem().comment.author != (this as CommentEntry).comment.author
                        }
                    }
        }

        private fun shouldShowAuthorAvatar(): Boolean {
            return with (itemsList.getOrNull(adapterPosition + 1)){
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

        val onCommentClickListener = OnClickListener { getItem().onClickedCallback.onClicked(getItem()) }

        override fun bindItem(item: CommentEntry) {
            super.bindItem(item)
            comment.setOnClickListener(onCommentClickListener)
            comment.status = when {
                getItem().hasError() -> Status.Error
                getItem().comment.isLocal() -> Status.Processing
                else -> Status.Completed
            }
            comment.contentType =
                    if (item.comment.hasAttachments()) ContentType.Attachment else ContentType.Text
            when (comment.contentType){
                ContentType.Text -> comment.setCommentText(getItem().comment.body)
                ContentType.Attachment -> bindAttachmentView()
            }
            creationTime.text = getTimeText(itemView.context, getItem().comment.creationDate)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            getItem().uploadFileHooks?.unsubscribeFromProgress()
        }

        private fun bindAttachmentView() {
            comment.fileProgressStatus = if (getItem().hasError()) Status.Error else Status.Completed
            comment.setFileName(getItem().comment.attachments?.first()?.name ?: "")
            comment.setFileSize(getItem().comment.attachments?.first()?.bytesSize?.toFloat() ?: 0f)
            comment.setOnProgressIconClickListener {
                when (comment.fileProgressStatus) {
                    Status.Processing -> getItem().uploadFileHooks?.cancelUploading()
                    Status.Completed -> onDownloadedFileClickListener?.invoke(getItem().comment.attachments!![0].id)
                    Status.Error -> comment.performClick()
                }
            }
            getItem().uploadFileHooks?.subscribeOnProgress {
                if (comment.fileProgressStatus != Status.Processing)
                    comment.fileProgressStatus = Status.Processing
                comment.setProgress(it)
            }
        }
    }

    private class DateViewHolder(parent: ViewGroup)
        : ViewHolderBase<DateEntry>(parent, R.layout.psd_view_holder_date) {

        val date = itemView.findViewById<TextView>(R.id.date)

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

            if (itemsList[viewHolder.adapterPosition].type != Type.Comment)
                return
            viewHolder as CommentHolder
            var x = dX
            if (x < -viewHolder.creationTime.width)
                x = -viewHolder.creationTime.width.toFloat()
            for (position in 0..(recyclerView.childCount - 1)) {
                recyclerView.findContainingViewHolder(recyclerView.getChildAt(position))?.let {
                    if (itemsList[it.adapterPosition].type != Type.Comment)
                        return@let
                    super.onChildDraw(
                            c,
                            recyclerView,
                            it,
                            x,
                            dY,
                            actionState,
                            false)
                }
            }
        }
    }
}
