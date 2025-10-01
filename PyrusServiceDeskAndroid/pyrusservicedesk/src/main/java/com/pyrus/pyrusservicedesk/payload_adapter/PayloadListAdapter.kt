package com.pyrus.pyrusservicedesk.payload_adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter


class PayloadListAdapter<Entry: Any>(
    vararg fingerprintList: ItemFingerprint<out Entry>,
) : ListAdapter<Entry, BaseViewHolder<Entry>>(PayloadDiffUtil<Entry>(fingerprintList)) {

    private val fingerprints = fingerprintList.associateBy { fingerprint -> fingerprint.entryKeyKClass }
    private val fingerprintByType = fingerprintList.associateBy { it.layoutId }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Entry> {
        val inflater = LayoutInflater.from(parent.context)
        return fingerprintByType[viewType]
            ?.getViewHolder(inflater, parent)
            ?.let { it as BaseViewHolder<Entry> }
            ?: throw IllegalArgumentException("View type not found: $viewType")
    }

    override fun getItemViewType(position: Int): Int {
        val item = currentList[position]
        return fingerprints[item::class]
            ?.layoutId
            ?:throw IllegalArgumentException("View type not found: $item")
    }

    override fun onBindViewHolder(holder: BaseViewHolder<Entry>, position: Int) {
        onBindViewHolder(holder, position, null)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        holder: BaseViewHolder<Entry>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val firstPayload = payloads.firstOrNull()
        if (firstPayload is Set<*>) {
            onBindViewHolder(holder, position, firstPayload as Set<String>?)
        }
        else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewRecycled(holder: BaseViewHolder<Entry>) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    private fun onBindViewHolder(holder: BaseViewHolder<Entry>, position: Int, payload: Set<String>?) {
        if (position !in 0 until currentList.size)
            return
        val actionBuilder = PayloadActionBuilder<Entry>(getItem(position), payload)
        holder.bind(actionBuilder)
        actionBuilder.start()
    }


}