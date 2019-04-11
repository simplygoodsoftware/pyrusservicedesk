package com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

/**
 * Base implementation of [RecyclerView.ViewHolder]
 *
 * @param parent parent view that is used to inflate given layout id
 * @param layoutId id of the layout of view holder's view
 */
internal abstract class ViewHolderBase<T>(
        parent: ViewGroup,
        @LayoutRes layoutId: Int)
    : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)){

    private var item: T? = null

    /**
     * Here implementations can bind data into its view.
     * NB: if implementation omits call of the super.bindItem hte actual [item] is not available then.
     */
    open fun bindItem(item: T) {
        this.item = item
    }

    /**
     * Callback that is invoked when view holder's view is detached from window.
     * This is convenient place to clear subscriptions and do things can cause problems related to
     * a view's lifecycle.
     */
    open fun onDetachedFromWindow() {}

    /**
     * Provides an item that was bound to a view holder in [bindItem].
     * NB: this call is safe only after super.bindItem is called by an implementation.
     */
    protected fun getItem() = item!!
}