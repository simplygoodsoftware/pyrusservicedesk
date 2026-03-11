package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.view.LayoutInflater
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.core.ResourceContextWrapper
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderDateBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass


internal class DateFingerprint() : ItemFingerprint<CommentEntry.Date>() {

    override val layoutId: Int = R.layout.psd_view_holder_date

    override val entryKeyKClass: KClass<*> = CommentEntry.Date::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.Date> {
        return DateViewHolder(
            PsdViewHolderDateBinding.inflate(layoutInflater, parent, false),
        )
    }

    override fun areItemsTheSame(
        oldItem: CommentEntry.Date,
        newItem: CommentEntry.Date
    ) = newItem.date == oldItem.date

    override fun getChangePayload(
        oldItem: CommentEntry.Date,
        newItem: CommentEntry.Date,
    ): Any? {
        val payload = HashSet<String>()
        if (oldItem.date != newItem.date) payload.add(getPropertyName(CommentEntry.Date::date))
        return payload
    }

}

internal class DateViewHolder(
    private val binding: PsdViewHolderDateBinding,
) : BaseViewHolder<CommentEntry.Date>(binding.root) {

    init {
        ConfigUtils.getMainFontTypeface()?.let {
            binding.date.typeface = it
        }
        binding.date.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(itemView.context))
    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.Date>) = builder.diff {
        this.entry::date.payloadCheck {
            binding.date.text = entry.date.text(binding.date.context)
        }
    }
}