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