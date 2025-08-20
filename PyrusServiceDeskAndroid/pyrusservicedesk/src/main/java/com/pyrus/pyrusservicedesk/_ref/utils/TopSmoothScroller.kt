package com.pyrus.pyrusservicedesk._ref.utils

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import kotlin.math.abs

class TopSmoothScroller(context: Context) : LinearSmoothScroller(context) {
    override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
        return -(layoutManager?.getDecoratedTop(view) ?: 0)
    }

    override fun calculateTimeForScrolling(dx: Int): Int {
        val childHeight = layoutManager?.getChildAt(0)?.height
        val allItemCount = layoutManager?.itemCount ?: 0
        if (childHeight != null && layoutManager?.itemCount != null) {
            var itemCount: Int = allItemCount - abs(dx) / childHeight
            if (itemCount < 1)
                itemCount = 1

            return if (itemCount <= 5) {
                super.calculateTimeForScrolling(dx) * itemCount
            } else {
                super.calculateTimeForScrolling(dx) * 2
            }
        }
        return super.calculateTimeForScrolling(dx)
    }
}