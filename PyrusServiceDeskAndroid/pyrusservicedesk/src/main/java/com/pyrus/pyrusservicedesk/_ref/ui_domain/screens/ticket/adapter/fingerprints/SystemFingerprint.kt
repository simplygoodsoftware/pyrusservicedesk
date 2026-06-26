package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.view.LayoutInflater
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderSystemMessageBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass


internal class SystemFingerprint : ItemFingerprint<CommentEntry.Comment.CommentSystemText>() {

    override val layoutId: Int = R.layout.psd_view_holder_system_message

    override val entryKeyKClass: KClass<*> = CommentEntry.Comment.CommentSystemText::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.Comment.CommentSystemText> {
        return CommentSystemTextViewHolder(
            PsdViewHolderSystemMessageBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun areItemsTheSame(
        oldItem: CommentEntry.Comment.CommentSystemText,
        newItem: CommentEntry.Comment.CommentSystemText
    ) = newItem.id == oldItem.id

    override fun getChangePayload(
        oldItem: CommentEntry.Comment.CommentSystemText,
        newItem: CommentEntry.Comment.CommentSystemText,
    ): Any? {
        val payload = HashSet<String>()

        if (oldItem.creationTime != newItem.creationTime) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::creationTime))
        if (oldItem.entryId != newItem.entryId) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::entryId))
        if (oldItem.id != newItem.id) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::id))
        if (oldItem.isInbound != newItem.isInbound) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::isInbound))
        if (oldItem.isSupport != newItem.isSupport) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::isSupport))
        if (oldItem.hasError != newItem.hasError) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::hasError))
        if (oldItem.isLocal != newItem.isLocal) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::isLocal))
        if (oldItem.isWelcomeMessage != newItem.isWelcomeMessage) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::isWelcomeMessage))
        if (oldItem.timeText != newItem.timeText) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::timeText))
        if (oldItem.status != newItem.status) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::status))
        if (oldItem.authorName != newItem.authorName) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::authorName))
        if (oldItem.authorKey != newItem.authorKey) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::authorKey))
        if (oldItem.showAuthorName != newItem.showAuthorName) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::showAuthorName))
        if (oldItem.avatarUrl != newItem.avatarUrl) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::avatarUrl))
        if (oldItem.showAvatar != newItem.showAvatar) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::showAvatar))
        if (oldItem.text != newItem.text) payload.add(getPropertyName(CommentEntry.Comment.CommentSystemText::text))

        return payload
    }

}

internal class CommentSystemTextViewHolder(
    private val binding: PsdViewHolderSystemMessageBinding
) : BaseViewHolder<CommentEntry.Comment.CommentSystemText>(binding.root) {

    init {
        ConfigUtils.getMainFontTypeface()?.let {
            binding.message.typeface = it
        }
        val backgroundColor = ConfigUtils.getSupportMessageTextBackgroundColor(binding.root.context)
        val textColor = ConfigUtils.getSupportMessageTextColor(binding.root.context, backgroundColor)
        binding.message.setTextColor(textColor)
    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.Comment.CommentSystemText>) = builder.diff {
        this.entry::text.payloadCheck {
            binding.message.text = entry.text
        }
    }
}