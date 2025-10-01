package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.flexbox.FlexboxLayoutManager
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry.ButtonEntry
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderButtonBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass


internal class ButtonSimpleFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ItemFingerprint<ButtonEntry.Simple>() {
    override val layoutId: Int = R.layout.psd_view_holder_button
    override val entryKeyKClass: KClass<*> = ButtonEntry.Simple::class
    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<ButtonEntry.Simple> {
        return ButtonSimpleHolder(
            PsdViewHolderButtonBinding.inflate(layoutInflater, parent, false),
            onEvent
        )
    }

    override fun areItemsTheSame(
        oldItem: ButtonEntry.Simple,
        newItem: ButtonEntry.Simple
    ) = true
}

internal class ButtonSimpleHolder(
    private val binding: PsdViewHolderButtonBinding,
    private val onEvent: (event: TicketView.Event) -> Unit,
) : BaseViewHolder<ButtonEntry.Simple>(binding.root) {

    private var text: String? = null
    init {
        val tint = ConfigUtils.getAccentColor(itemView.context)
        binding.btnButton.background.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
        binding.btnButton.setTextColor(ConfigUtils.getAccentColor(itemView.context))
        binding.btnButton.setOnClickListener {
            onEvent(TicketView.Event.OnButtonClick(text ?: ""))
        }
    }

    override fun bind(builder: PayloadActionBuilder<ButtonEntry.Simple>) = builder.diff {
        text = entry.text
        this.entry::text.payloadCheck {
            val lp = binding.btnButton.layoutParams
            if (lp is FlexboxLayoutManager.LayoutParams) {
                lp.flexGrow = 1f
            }
            binding.btnButton.text = entry.text
        }
    }
}