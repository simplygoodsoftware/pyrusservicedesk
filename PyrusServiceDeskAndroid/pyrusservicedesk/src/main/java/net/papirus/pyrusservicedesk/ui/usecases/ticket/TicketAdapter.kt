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
import net.papirus.pyrusservicedesk.repository.data.Comment
import net.papirus.pyrusservicedesk.ui.view.CommentView
import net.papirus.pyrusservicedesk.ui.view.ContentType
import net.papirus.pyrusservicedesk.ui.view.recyclerview.AdapterBase
import net.papirus.pyrusservicedesk.ui.view.recyclerview.ViewHolderBase
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

            // TODO
            setAuthorVisibility(false)
        }

        private fun setAuthorVisibility(visible: Boolean) {
            avatar.visibility = if (visible) VISIBLE else INVISIBLE
            authorName.visibility = if (visible) VISIBLE else GONE
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
            creationTime.text = getTimeText(itemView.context, (getItem().getCreationDate()))
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
