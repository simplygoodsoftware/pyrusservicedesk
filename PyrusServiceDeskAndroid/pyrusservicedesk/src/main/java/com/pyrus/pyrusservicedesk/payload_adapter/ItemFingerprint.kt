package com.pyrus.pyrusservicedesk.payload_adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import kotlin.reflect.KClass

abstract class ItemFingerprint<Entry: Any> {

    abstract val layoutId: Int

    abstract val entryKeyKClass: KClass<*>

    abstract fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<Entry>

    abstract fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean

    open fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
        return oldItem == newItem
    }

}