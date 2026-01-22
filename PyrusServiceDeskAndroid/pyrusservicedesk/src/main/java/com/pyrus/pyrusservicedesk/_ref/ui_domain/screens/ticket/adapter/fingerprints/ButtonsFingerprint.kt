package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry.ButtonEntry
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderButtonsBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadListAdapter
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass


internal class ButtonsFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ItemFingerprint<CommentEntry.Buttons>() {
    override val layoutId: Int = R.layout.psd_view_holder_buttons
    override val entryKeyKClass: KClass<*> = CommentEntry.Buttons::class
    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.Buttons> {
        return ButtonsHolder(
            PsdViewHolderButtonsBinding.inflate(layoutInflater, parent, false),
            onEvent
        )
    }

    override fun areItemsTheSame(
        oldItem: CommentEntry.Buttons,
        newItem: CommentEntry.Buttons
    ) = newItem.id == oldItem.id

    override fun getChangePayload(
        oldItem: CommentEntry.Buttons,
        newItem: CommentEntry.Buttons,
    ): Any? {
        val payload = HashSet<String>()

        if (oldItem.creationTime != newItem.creationTime) payload.add(getPropertyName(CommentEntry.Buttons::creationTime))
        if (oldItem.id != newItem.id) payload.add(getPropertyName(CommentEntry.Buttons::id))
        if (oldItem.buttons != newItem.buttons) payload.add(getPropertyName(CommentEntry.Buttons::buttons))

        return payload
    }
}

internal class ButtonsHolder(
    binding: PsdViewHolderButtonsBinding,
    private val onEvent: (event: TicketView.Event) -> Unit,
) : BaseViewHolder<CommentEntry.Buttons>(binding.root) {

    private val flexboxLayoutManager = FlexboxLayoutManager(binding.root.context).apply {
        flexWrap = FlexWrap.WRAP
        flexDirection = FlexDirection.ROW
        alignItems = AlignItems.STRETCH
        justifyContent = JustifyContent.FLEX_END
    }

    private val adapter: PayloadListAdapter<ButtonEntry> by lazy { PayloadListAdapter(
        ButtonSimpleFingerprint(onEvent),
        ButtonLinkFingerprint(),
    ) }

    init {
        binding.buttonsRecyclerview.adapter = adapter
        binding.buttonsRecyclerview.layoutManager = flexboxLayoutManager
    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.Buttons>) = builder.diff {
        this.entry::buttons.payloadCheck {
            adapter.submitList(entry.buttons)
        }
    }

}