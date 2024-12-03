package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.WelcomeMessageEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils

internal class WelcomeMessageHolder(parent: ViewGroup) :
    ViewHolderBase<WelcomeMessageEntry>(parent, R.layout.psd_view_holder_comment_inbound) {

    private val comment: CommentView = itemView.findViewById(R.id.comment)
    private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
    private val authorName = itemView.findViewById<TextView>(R.id.author_name)

    init {
        ConfigUtils.getMainFontTypeface()?.let {
            authorName.typeface = it
        }
    }

    override fun bindItem(item: WelcomeMessageEntry) {
        super.bindItem(item)
        authorName.visibility = View.GONE
        avatar.visibility = View.INVISIBLE
        comment.contentType = ContentType.Text
        comment.setCommentText(item.message)
    }
}