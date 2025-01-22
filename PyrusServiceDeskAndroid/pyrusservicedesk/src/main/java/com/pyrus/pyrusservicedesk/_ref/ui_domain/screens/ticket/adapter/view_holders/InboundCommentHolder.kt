package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView

internal class InboundCommentHolder(
    parent: ViewGroup,
    onEvent: (event: TicketView.Event) -> Unit,
) : CommentHolder(
    parent,
    R.layout.psd_view_holder_comment_inbound,
    onEvent
) {

    override val comment: CommentView = itemView.findViewById(R.id.comment)
    private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
    private val authorName = itemView.findViewById<TextView>(R.id.author_name)

    init {
        ConfigUtils.getMainFontTypeface()?.let {
            authorName.typeface = it
        }
        authorName.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(parent.context))
    }

    override fun bindItem(entry: CommentEntryV2.Comment) {
        super.bindItem(entry)
        setAuthorNameAndVisibility(entry, entry.showAuthorName)
        setAuthorAvatarVisibility(entry, entry.showAvatar)
    }

    private fun setAuthorNameAndVisibility(item: CommentEntryV2.Comment, visible: Boolean) {
        authorName.visibility = if (visible) View.VISIBLE else View.GONE
        authorName.text = item.authorName
    }

    private fun setAuthorAvatarVisibility(item: CommentEntryV2.Comment, visible: Boolean) {
        avatar.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        if (visible) {
            PyrusServiceDesk.injector().picasso
                .load(item.avatarUrl)
                .placeholder(ConfigUtils.getSupportAvatar(itemView.context))
                .transform(CIRCLE_TRANSFORMATION)
                .into(avatar)
        }
    }

}