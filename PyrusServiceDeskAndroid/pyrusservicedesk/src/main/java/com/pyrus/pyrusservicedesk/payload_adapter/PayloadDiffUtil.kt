package com.pyrus.pyrusservicedesk.payload_adapter

import androidx.recyclerview.widget.DiffUtil
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

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

    override fun getChangePayload(oldItem: Entry, newItem: Entry): Any? {
        if (oldItem::class == newItem::class) {
            val payload = HashSet<String>()

            val oldValues: Map<String, Any?> = oldItem.extract()
            val newValues: Map<String, Any?> = newItem.extract()

            for (entry in newValues) {
                if (oldValues[entry.key] != entry.value) {
                    payload.add(entry.key)
                }
            }

            return payload
        }
        return super.getChangePayload(oldItem, newItem)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> T.extract(): Map<String, Any?> {

        val publicProps = (this::class as KClass<T>).declaredMemberProperties.filter {
            it.visibility == KVisibility.PUBLIC || it.visibility == KVisibility.INTERNAL
        }
        return publicProps.associate { p -> p.name to p.get(this) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getFingerprint(item: Entry): ItemFingerprint<Entry> {
        return fingerprints[item::class]
            ?.let { it as ItemFingerprint<Entry> }
            ?: throw IllegalStateException("Fingerprint not found for $item")
    }

}