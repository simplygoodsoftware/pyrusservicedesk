package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.adjustColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.getTextColorOnBackground
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentSimpleTextBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass

internal class SimpleTextFingerprint: ItemFingerprint<CommentEntry.SimpleText>() {

    override val layoutId: Int = R.layout.psd_view_holder_comment_simple_text

    override val entryKeyKClass: KClass<*> = CommentEntry.SimpleText::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.SimpleText> = WelcomeMessageHolder(
        PsdViewHolderCommentSimpleTextBinding.inflate(layoutInflater, parent, false)
    )

    override fun areItemsTheSame(
        oldItem: CommentEntry.SimpleText,
        newItem: CommentEntry.SimpleText
    ) = newItem.entryId == oldItem.entryId

    override fun getChangePayload(
        oldItem: CommentEntry.SimpleText,
        newItem: CommentEntry.SimpleText,
    ): Any? {
        val payload = HashSet<String>()
        if (oldItem.creationTime != newItem.creationTime) payload.add(getPropertyName(CommentEntry.SimpleText::creationTime))
        if (oldItem.entryId != newItem.entryId) payload.add(getPropertyName(CommentEntry.SimpleText::entryId))
        if (oldItem.message != newItem.message) payload.add(getPropertyName(CommentEntry.SimpleText::message))
        if (oldItem.avatarUrl != newItem.avatarUrl) payload.add(getPropertyName(CommentEntry.SimpleText::avatarUrl))
        return payload
    }
}

internal class WelcomeMessageHolder(
    private val binding: PsdViewHolderCommentSimpleTextBinding
) : BaseViewHolder<CommentEntry.SimpleText>(binding.root) {

    init {
        ConfigUtils.getMainFontTypeface()?.let {
            binding.authorName.typeface = it
        }
        binding.comment.commentTextLayout.visibility = View.VISIBLE
        binding.authorName.visibility = View.GONE
        binding.avatar.visibility = View.VISIBLE

        val backgroundColor = ConfigUtils.getSupportMessageTextBackgroundColor(binding.root.context)
        val textColor = ConfigUtils.getSupportMessageTextColor(binding.root.context, backgroundColor)
        val primaryColor = getTextColorOnBackground(binding.root.context, backgroundColor)
        val secondaryColor = adjustColorChannel(primaryColor, ColorChannel.Alpha, SECONDARY_TEXT_COLOR_MULTIPLIER)

        binding.comment.backgroundParent.setCardBackgroundColor(backgroundColor)
        binding.comment.commentText.setTextColor(textColor)
        binding.comment.textTime.setTextColor(secondaryColor)

    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.SimpleText>) = builder.diff {

        this.entry::message.payloadCheck {
            setCommentText(entry.message)
        }
        this.entry::avatarUrl.payloadCheck {
            PyrusServiceDesk.injector().picasso
                .load(entry.avatarUrl)
                .placeholder(ConfigUtils.getSupportAvatar(itemView.context))
                .transform(CIRCLE_TRANSFORMATION)
                .into(binding.avatar)
        }
    }

    /**
         * Assigns [text] of the comments.
         * Works with [ContentType.Text].
         */
    private fun setCommentText(text: String) {
        val filteredText = text
        binding.comment.commentText.text = filteredText
    }

    companion object {
        private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
    }
}