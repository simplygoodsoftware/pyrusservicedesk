package net.papirus.pyrusservicedesk.ui.view.recyclerview

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

internal abstract class ViewHolderBase<T>(
        parent: ViewGroup,
        @LayoutRes layoutId: Int)
    : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)){

    private var item: T? = null

    open fun bindItem(item: T) {
        this.item = item
    }

    protected fun getItem() = item!!
}