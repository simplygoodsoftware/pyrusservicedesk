package net.papirus.pyrusservicedesk.utils

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

/**
 * Checks whether given recyclerview is at end.
 *
 * NB: applicable to recyclerviews that have [LinearLayoutManager] only
 */
internal fun RecyclerView.isAtEnd(): Boolean {
    return adapter == null
            || layoutManager == null
            || childCount == 0
            || (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == adapter!!.itemCount - 1
}