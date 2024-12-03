package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.ViewGroup
import android.widget.TextView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.DateEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.utils.ConfigUtils

internal class DateViewHolder(parent: ViewGroup)
    : ViewHolderBase<DateEntry>(parent, R.layout.psd_view_holder_date) {

    private val date = itemView.findViewById<TextView>(R.id.date)

    init {
        ConfigUtils.getMainFontTypeface()?.let {
            date.typeface = it
        }
        date.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(itemView.context))
    }

    override fun bindItem(item: DateEntry) {
        super.bindItem(item)
        date.text = item.date
    }
}