package com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper


/**
 * Base [RecyclerView.Adapter] implementation.
 */
internal abstract class AdapterBase<Item> : RecyclerView.Adapter<ViewHolderBase<Item>>() {

    /**
     * List of items that are rendered by adapter.
     */
    protected var itemsList: MutableList<Item> = mutableListOf()
    /**
     * [ItemTouchHelper] implementation that can be provided by the extender.
     */
    open val itemTouchHelper: ItemTouchHelper? = null

    override fun getItemCount(): Int {
        return itemsList.size
    }

    override fun onBindViewHolder(holder: ViewHolderBase<Item>, position: Int) {
        holder.bindItem(itemsList[position])
    }

    override fun onViewDetachedFromWindow(holder: ViewHolderBase<Item>) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetachedFromWindow()
    }

    /**
     * Applies [items] as current rendered list of items. [notifyDataSetChanged] is called by default when new
     * list is applied.
     */
    open fun setItems(items: List<Item>) {
        this.itemsList = items.toMutableList()
        notifyDataSetChanged()
    }
}