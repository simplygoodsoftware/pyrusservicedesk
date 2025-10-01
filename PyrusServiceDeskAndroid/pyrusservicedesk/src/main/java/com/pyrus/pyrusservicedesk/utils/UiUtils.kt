package com.pyrus.pyrusservicedesk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

internal object UiUtils {

    fun hideKeyboard(view: View) {
        val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
        (inputMethodManager as? InputMethodManager)?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @JvmStatic
    fun dpToPx(dp: Float): Float {
        return dp * Resources.getSystem().displayMetrics.density
    }

    @JvmStatic
    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun calculateValue(progress: Float, start: Float, end: Float): Float {
        val clampedProgress = progress.coerceIn(0f, 1f)
        return start + (end - start) * clampedProgress
    }

}