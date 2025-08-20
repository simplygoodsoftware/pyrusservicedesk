package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.ticket_list

import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.setTimeoutClickListener
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView
import com.pyrus.pyrusservicedesk._ref.utils.CenteredImageSpan
import com.pyrus.pyrusservicedesk._ref.utils.animateVisibility
import com.pyrus.pyrusservicedesk._ref.utils.getTimeWhen
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListItemBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import java.util.Calendar
import kotlin.reflect.KClass

/**
Fingerprint for showing ticket headers in rv
 */
internal class TicketHeadersListFingerprint(
    private val onEvent: (TicketsContract.Message.Outer) -> Unit,
) : ItemFingerprint<TicketsView.Model.TicketsEntry.TicketHeaderEntry>() {

    override val layoutId: Int = R.layout.psd_tickets_list_item

    override val entryKeyKClass: KClass<*> = TicketsView.Model.TicketsEntry.TicketHeaderEntry::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<TicketsView.Model.TicketsEntry.TicketHeaderEntry> {
        return TicketsListViewHolder(
            PsdTicketsListItemBinding.inflate(layoutInflater, parent, false),
            onEvent
        )
    }

    override fun areItemsTheSame(
        oldItem: TicketsView.Model.TicketsEntry.TicketHeaderEntry,
        newItem: TicketsView.Model.TicketsEntry.TicketHeaderEntry
    ) = oldItem.ticketId == newItem.ticketId

}

internal class TicketsListViewHolder(
    private val binding: PsdTicketsListItemBinding,
    onEvent: (TicketsContract.Message.Outer) -> Unit
) : BaseViewHolder<TicketsView.Model.TicketsEntry.TicketHeaderEntry>(binding.root) {

    private var currentTicketHeader: TicketsView.Model.TicketsEntry.TicketHeaderEntry? = null

    init {
        binding.root.setTimeoutClickListener {
            currentTicketHeader?.let {
                onEvent.invoke(TicketsContract.Message.Outer.OnTicketClick(it.ticketId, it.userId))
            }
        }
    }

    override fun bind(builder: PayloadActionBuilder<TicketsView.Model.TicketsEntry.TicketHeaderEntry>) =
        builder.diff {
            currentTicketHeader = entry

            // TODO форматированае и спаны
            this.entry::title.payloadCheck {
                binding.ticketTitleTv.text = entry.title.text(itemView.context)
            }

            // TODO форматированае и спаны
            this.entry::lastCommentText.payloadCheck {
                val drawable: Drawable? = entry.lastCommentIconRes?.let {
                    AppCompatResources.getDrawable(
                        binding.root.context,
                        entry.lastCommentIconRes
                    )
                }
                val text = SpannableString(entry.lastCommentText?.text(binding.root.context) ?: "")

                drawable?.let { drawable ->
                    val size = (24 * binding.root.resources.displayMetrics.density).toInt()
                    drawable.setBounds(0, 0, size, size)

                    val imageSpan = CenteredImageSpan(drawable)

                    val startPosition = text.indexOf("[icon]")
                    val endPosition = startPosition + "[icon]".length

                    text.setSpan(
                        imageSpan,
                        startPosition,
                        endPosition,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )


                }
                binding.ticketCommentTv.text = text
            }

            this.entry::lastCommentCreationTime.payloadCheck {
                val lastCommentCreationTime = entry.lastCommentCreationTime
                val dateText =
                    lastCommentCreationTime?.getTimeWhen(itemView.context, Calendar.getInstance())
                binding.ticketTimeTv.text = dateText
            }

            this.entry::isRead.payloadCheck {
                if (firstItemBind) binding.ticketUnreadIv.isVisible = !entry.isRead
                else binding.ticketUnreadIv.animateVisibility(!entry.isRead)
            }
            this.entry::isLoading.payloadCheck {
                if (firstItemBind) binding.ticketStatusIv.isVisible = entry.isLoading
                else binding.ticketStatusIv.animateVisibility(entry.isLoading)

                val animation = binding.ticketStatusIv.drawable as? AnimationDrawable
                if (entry.isLoading) animation?.start()
                else animation?.stop()
            }
        }
}