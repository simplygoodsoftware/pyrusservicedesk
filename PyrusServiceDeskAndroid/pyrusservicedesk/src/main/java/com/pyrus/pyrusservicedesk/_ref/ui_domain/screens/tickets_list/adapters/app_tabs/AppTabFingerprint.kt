package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.app_tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TabEntry
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.animateTextColor
import com.pyrus.pyrusservicedesk._ref.utils.animateVisibility
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsTabBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass

/**
 * Fingerprint for showing vendor tabs
 */
internal class AppTabFingerprint(
    private val onEvent: (TicketsContract.Message.Outer) -> Unit,
) : ItemFingerprint<TabEntry>() {

    override val layoutId: Int = R.layout.psd_tickets_tab

    override val entryKeyKClass: KClass<*> = TabEntry::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<TabEntry> = TabViewHolder(
        PsdTicketsTabBinding.inflate(layoutInflater, parent, false),
        onEvent
    )

    override fun areItemsTheSame(oldItem: TabEntry, newItem: TabEntry) =
        oldItem.appId == newItem.appId

}

private class TabViewHolder(
    private val binding: PsdTicketsTabBinding,
    onEvent: (TicketsContract.Message.Outer) -> Unit,
) : BaseViewHolder<TabEntry>(binding.root) {

    var currentAppId: String? = null

    init {
        binding.tabLine.setBackgroundColor(ConfigUtils.Companion.getAccentColor(binding.tabLine.context))
        itemView.setOnClickListener {
            currentAppId?.let {
                onEvent.invoke(TicketsContract.Message.Outer.OnChangePage(it))
            }
        }
    }

    override fun bind(builder: PayloadActionBuilder<TabEntry>) = builder.diff {
        currentAppId = entry.appId

        entry::titleText.payloadCheck {
            binding.tabText.text = entry.titleText
        }
        entry::isSelected.payloadCheck {
            if (entry.isSelected) {
                val accentColor = ConfigUtils.Companion.getAccentColor(itemView.context)
                if (firstItemBind) binding.tabText.setTextColor(accentColor)
                else binding.tabText.animateTextColor(accentColor, ANIMATION_DURATION)
            }
            else {
                val textColor = itemView.resources.getColor(R.color.psd_text_2)
                if (firstItemBind) binding.tabText.setTextColor(textColor)
                else binding.tabText.animateTextColor(textColor, ANIMATION_DURATION)
            }

            if (firstItemBind) binding.tabLine.visibility = if (entry.isSelected) View.VISIBLE else View.INVISIBLE
            else binding.tabLine.animateVisibility(entry.isSelected, ANIMATION_DURATION)
        }
    }

    companion object {
        const val ANIMATION_DURATION = 200L
    }

}