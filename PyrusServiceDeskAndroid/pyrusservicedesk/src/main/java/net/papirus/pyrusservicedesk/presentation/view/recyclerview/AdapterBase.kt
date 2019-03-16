package net.papirus.pyrusservicedesk.presentation.view.recyclerview

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper


internal abstract class AdapterBase<Item> : RecyclerView.Adapter<ViewHolderBase<Item>>() {

    protected var itemsList: MutableList<Item> = mutableListOf()
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

    fun setItems(items: List<Item>) {
        this.itemsList = items.toMutableList()
        notifyDataSetChanged()
    }

    fun setItemsWithoutUpdate(items: List<Item>) {
        this.itemsList = items.toMutableList()
    }

    fun appendItem(item: Item) {
        itemsList.add(item)
        notifyItemInserted(itemsList.lastIndex)
    }
}