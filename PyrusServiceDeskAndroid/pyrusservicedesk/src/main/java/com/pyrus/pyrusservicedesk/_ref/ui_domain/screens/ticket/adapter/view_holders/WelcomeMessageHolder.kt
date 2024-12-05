package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.WelcomeMessageEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentInboundBinding
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderRatingBinding

internal class WelcomeMessageHolder(parent: ViewGroup) :
    ViewHolderBase<CommentEntryV2.WelcomeMessage>(parent, R.layout.psd_view_holder_comment_inbound) {

    private val binding = PsdViewHolderCommentInboundBinding.bind(itemView)

    init {
        ConfigUtils.getMainFontTypeface()?.let {
            binding.authorName.typeface = it
        }
        binding.authorName.visibility = View.GONE
        binding.avatar.visibility = View.INVISIBLE
        binding.comment.contentType = ContentType.Text
    }

    override fun bindItem(entry: CommentEntryV2.WelcomeMessage) {
        super.bindItem(entry)

        binding.comment.setCommentText(entry.message)
    }
}