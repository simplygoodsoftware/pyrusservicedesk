package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters

import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.MainThread

internal class TimeoutListener(private val onClick: () -> Unit): OnClickListener {

    @MainThread
    override fun onClick(v: View?) {
        val currentTime = System.currentTimeMillis()
        if (System.currentTimeMillis() - lastClickTime > 500L) {
            lastClickTime = currentTime
            onClick()
        }
    }

    companion object {
        private var lastClickTime = 0L
    }

}

internal fun View.setTimeoutClickListener(onClick: () -> Unit) {
    setOnClickListener(TimeoutListener(onClick))
}