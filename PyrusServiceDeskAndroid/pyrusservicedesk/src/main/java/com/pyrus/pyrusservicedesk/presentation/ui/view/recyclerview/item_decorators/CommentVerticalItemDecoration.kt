package com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadListAdapter

internal class CommentVerticalItemDecoration(
    private val innerDivider: Int,
    private val outerDivider: Int,
    private val invert: Boolean = false,
    private val needSpecialOuterSpace: (currentEntry: CommentEntry?, nextEntry: CommentEntry?) -> Boolean,
    ) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {

        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION || position == parent.adapter?.itemCount?.minus(1)) return

        val adapter1 = parent.adapter as? PayloadListAdapter<CommentEntry> ?: return
        val currentEntry = adapter1.currentList[position] ?: return
        val nextEntry = adapter1.currentList[position + 1] ?: return

        val spacing =
            if (needSpecialOuterSpace(currentEntry, nextEntry)) outerDivider else innerDivider


        if (invert) with(outRect) {
            top = spacing
        }
        else with(outRect) {
            bottom = spacing
        }
    }
}