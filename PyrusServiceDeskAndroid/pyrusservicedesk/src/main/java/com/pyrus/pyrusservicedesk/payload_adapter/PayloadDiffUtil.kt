package com.pyrus.pyrusservicedesk.payload_adapter

import androidx.recyclerview.widget.DiffUtil

class PayloadDiffUtil<Entry: Any>(
    fingerprintList: Array<out ItemFingerprint<out Entry>>,
) : DiffUtil.ItemCallback<Entry>() {

    private val fingerprints = fingerprintList.associateBy { it.entryKeyKClass }

    override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean {
        if (oldItem::class != newItem::class) return false

        return getFingerprint(oldItem).areItemsTheSame(oldItem, newItem)
    }

    override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
        if (oldItem::class != newItem::class) return false
        return getFingerprint(oldItem).areContentsTheSame(oldItem, newItem)
    }

    /*
    Used to determine the payload, but overridden in each fingerprint separately
    to explicitly compare the content for each entry.
    Reflection was removed due to this
    https://pyrus.com/t#kb/article/ploxoe-obnovlenie-ui-v-servicedesk-H8uF3uLJc9w
     */
    override fun getChangePayload(oldItem: Entry, newItem: Entry): Any? {
        if (oldItem::class != newItem::class) return super.getChangePayload(oldItem, newItem)
        return getFingerprint(oldItem).getChangePayload(oldItem, newItem)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getFingerprint(item: Entry): ItemFingerprint<Entry> {
        return fingerprints[item::class]
            ?.let { it as ItemFingerprint<Entry> }
            ?: throw IllegalStateException("Fingerprint not found for $item")
    }

}