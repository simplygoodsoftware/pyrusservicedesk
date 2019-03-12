package net.papirus.pyrusservicedesk.ui.usecases.ticket

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
import net.papirus.pyrusservicedesk.repository.data.Comment
import net.papirus.pyrusservicedesk.repository.web_service.getAvatarUrl
import net.papirus.pyrusservicedesk.ui.view.CommentView
import net.papirus.pyrusservicedesk.ui.view.ContentType
import net.papirus.pyrusservicedesk.ui.view.recyclerview.AdapterBase
import net.papirus.pyrusservicedesk.ui.view.recyclerview.ViewHolderBase
import net.papirus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import net.papirus.pyrusservicedesk.utils.getSimpleAvatar
import net.papirus.pyrusservicedesk.utils.getTimeText


private const val VIEW_TYPE_COMMENT_INBOUND = 0
private const val VIEW_TYPE_COMMENT_OUTBOUND = 1

internal class TicketAdapter: AdapterBase<Comment>() {

    override val itemTouchHelper: ItemTouchHelper? = ItemTouchHelper(TouchCallback())

    override fun getItemViewType(position: Int): Int {
        return with(itemsList[position]) {
            return@with when {
                isInbound -> VIEW_TYPE_COMMENT_OUTBOUND
                else -> VIEW_TYPE_COMMENT_INBOUND
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<Comment> {
        return when(viewType){
            VIEW_TYPE_COMMENT_INBOUND -> InboundCommentHolder(parent)
            else -> OutboundCommentHolder(parent)
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolderBase<Comment>) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.translationX = 0f
    }

    private inner class InboundCommentHolder(parent: ViewGroup)
        : CommentHolder(parent, R.layout.psd_view_holder_comment_inbound){

        override val comment: CommentView = itemView.findViewById(R.id.comment)
        override val creationTime: TextView = itemView.findViewById(R.id.creation_time)
        private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        private val authorName = itemView.findViewById<TextView>(R.id.author_name)

        override fun bindItem(item: Comment) {
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
                authorName.text = getItem().author.name
        }

        private fun setAuthorAvatarVisibility(visible: Boolean) {
            avatar.visibility = if (visible) VISIBLE else INVISIBLE
            if (visible) {
                Picasso.get()
                    .load(getAvatarUrl(getItem().author.avatarId))
                    .placeholder(getSimpleAvatar(itemView.context, getItem().author))
                    .transform(CIRCLE_TRANSFORMATION)
                    .into(avatar)
            }
        }

        private fun shouldShowAuthorName(): Boolean {
            return adapterPosition == 0 || getItem().author != itemsList[adapterPosition - 1].author
        }

        private fun shouldShowAuthorAvatar(): Boolean {
            return getItem().author != itemsList.getOrNull(adapterPosition + 1)?.author
        }
    }

    private inner class OutboundCommentHolder(parent: ViewGroup)
        : CommentHolder(parent, R.layout.psd_view_holder_comment_outbound){

        override val comment: CommentView = itemView.findViewById(R.id.comment)
        override val creationTime: TextView = itemView.findViewById(R.id.creation_time)
    }

    private abstract class CommentHolder(
            parent: ViewGroup,
            @LayoutRes layoutRes: Int)
        : ViewHolderBase<Comment>(parent, layoutRes){

        abstract val comment: CommentView
        abstract val creationTime: TextView

        override fun bindItem(item: Comment) {
            super.bindItem(item)
            comment.contentType =
                    if (item.hasAttachments()) ContentType.Attachment else ContentType.Text
            when (comment.contentType){
                ContentType.Text -> comment.setCommentText(getItem().body)
                ContentType.Attachment -> bindAttachmentView()
            }
            creationTime.text = getTimeText(itemView.context, getItem().creationDate)
        }

        private fun bindAttachmentView() {
            comment.setFileName(getItem().attachments?.first()?.name ?: "")
            comment.setFileSize(getItem().attachments?.first()?.bytesSize?.toFloat() ?: 0f)
            comment.setOnDownloadIconClickListener {

            }
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

            var x = dX
            if (x < -(viewHolder as CommentHolder).creationTime.width)
                x = -viewHolder.creationTime.width.toFloat()
            for (position in 0..(recyclerView.childCount - 1)) {
                recyclerView.findContainingViewHolder(recyclerView.getChildAt(position))?.let {
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
