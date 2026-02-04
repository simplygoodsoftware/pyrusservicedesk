package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.ticket_list

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListClosedItemBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass

/**
 * Fingerprint for showing closed ticket header in rv
 */
internal class ClosedTicketsTitleFingerprint(
    private val onEvent: (TicketsContract.Message.Outer) -> Unit,
) : ItemFingerprint<TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry>() {

    override val layoutId: Int = R.layout.psd_tickets_list_closed_item

    override val entryKeyKClass: KClass<*> = TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry> {
        return ClosedTicketsTitleViewHolder(
            PsdTicketsListClosedItemBinding.inflate(layoutInflater, parent, false),
            onEvent
        )
    }

    override fun areItemsTheSame(
        oldItem: TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry,
        newItem: TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry
    ) = true

    override fun getChangePayload(
        oldItem: TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry,
        newItem: TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry,
    ): Any? {
        val payload = HashSet<String>()
        if (oldItem.isExpanded != newItem.isExpanded) payload.add(getPropertyName(TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry::isExpanded))
        if (oldItem.count != newItem.count) payload.add(getPropertyName(TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry::count))

        return payload
    }
}

private class ClosedTicketsTitleViewHolder(
    private val binding: PsdTicketsListClosedItemBinding,
    onEvent: (TicketsContract.Message.Outer) -> Unit
) : BaseViewHolder<TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry>(binding.root) {

    private var isExpanded = false

    init {
        itemView.setBackgroundColor(itemView.resources.getColor(R.color.psd_comment_inbound_bg))
        itemView.setOnClickListener {
            if (isExpanded) {
                binding.closedTicketsArrowIv.animate().rotation(0f)
            } else {
                binding.closedTicketsArrowIv.animate().rotation(180f)
            }
            onEvent.invoke(
                TicketsContract.Message.Outer.OnClosedTicketsTitleCLick
            )
        }
    }

    override fun bind(builder: PayloadActionBuilder<TicketsView.Model.TicketsEntry.ClosedTicketTitleEntry>) = builder.diff {
        isExpanded = entry.isExpanded

        this.entry::count.payloadCheck {
            val resString = ContextCompat.getString(itemView.context, R.string.closed_teckets)
            val string = SpannableString("$resString ${entry.count}")
            string.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.psd_color_bluegray_500
                    )
                ), resString.length, string.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.closedTicketsTitleTv.text = string
        }

        this.entry::isExpanded.payloadCheck {
            binding.closedTicketsArrowIv.rotation = if (entry.isExpanded) 180f else 0f
        }
    }
}