package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.RequestUtils

internal class InboundCommentHolder(
    parent: ViewGroup,
    onErrorCommentEntryClickListener: (entry: CommentEntry) -> Unit,
    onFileReadyToPreviewClickListener: (attachment: Attachment) -> Unit,
    onTextCommentLongClicked: (String) -> Unit,
) : CommentHolder(
    parent,
    R.layout.psd_view_holder_comment_inbound,
    onErrorCommentEntryClickListener,
    onFileReadyToPreviewClickListener,
    onTextCommentLongClicked
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

    override fun bindItem(item: CommentEntry) {
        super.bindItem(item)
        setAuthorNameAndVisibility(true)
        setAuthorAvatarVisibility(true)
    }

    private fun setAuthorNameAndVisibility(visible: Boolean) {
        authorName.visibility = if (visible) View.VISIBLE else View.GONE
        authorName.text = getItem().comment.author.name
    }

    private fun setAuthorAvatarVisibility(visible: Boolean) {
        avatar.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        if (visible) {
            PyrusServiceDesk.injector().picasso
                .load(
                    RequestUtils.getAvatarUrl(
                        getItem().comment.author.avatarId,
                        PyrusServiceDesk.get().domain
                    )
                )
                .placeholder(ConfigUtils.getSupportAvatar(itemView.context))
                .transform(CIRCLE_TRANSFORMATION)
                .into(avatar)
        }
    }

}