package com.pyrus.pyrusservicedesk.payload_adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

open class BaseViewHolder<Entry>(root: View) : RecyclerView.ViewHolder(root) {

    open fun bind(builder: PayloadActionBuilder<Entry>) {

    }

    open fun clear() {

    }



    //fun TextProvider.text() = text(itemView.context)

}