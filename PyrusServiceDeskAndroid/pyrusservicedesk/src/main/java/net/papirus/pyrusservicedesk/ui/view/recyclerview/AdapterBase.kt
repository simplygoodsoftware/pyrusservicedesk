package net.papirus.pyrusservicedesk.ui.view.recyclerview

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper


internal abstract class AdapterBase<Item>: RecyclerView.Adapter<ViewHolderBase<Item>>() {

    protected var itemsList: MutableList<Item> = mutableListOf()
    open val itemTouchHelper: ItemTouchHelper? = null

    fun setItems(items: List<Item>) {
        this.itemsList = items.toMutableList()
        notifyDataSetChanged()
    }

    fun getItems(): List<Item> {
        return itemsList
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    override fun onBindViewHolder(holder: ViewHolderBase<Item>, position: Int) {
        holder.bindItem(itemsList[position])
    }

    fun appendItems(items: List<Item>){
        val sizeBefore = itemsList.size
        itemsList.addAll(items)
        notifyItemRangeInserted(sizeBefore, items.size)
    }
}