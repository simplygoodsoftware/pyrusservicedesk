package com.pyrus.pyrusservicedesk._ref.utils

import android.app.Activity
import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private const val SHOW_KEYBOARD_RETRY_DELAY_MS = 100L

/**
 * Checks whether given recyclerview is at end.
 *
 * NB: applicable to recyclerviews that have [LinearLayoutManager] only
 */
internal fun RecyclerView.isAtEnd(): Boolean {
    return adapter == null
            || layoutManager == null
            || childCount == 0
            || (layoutManager as LinearLayoutManager).findLastVisibleItemPosition() == adapter!!.itemCount - 1
}

internal fun Activity.setupWindowInsets(rootView: FrameLayout) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        val keyboardInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val imeHeight = max(keyboardInsets.bottom - systemBarInsets.bottom, 0)

//        view.setPadding(
//            systemInsets.left,
//            systemInsets.top,
//            systemInsets.right,
//            systemInsets.bottom + imeHeight
//        )

        rootView.updateLayoutParams<FrameLayout.LayoutParams> {
            leftMargin = systemInsets.left
            topMargin = systemInsets.top
            rightMargin = systemInsets.right
            bottomMargin = systemInsets.bottom + imeHeight
        }
//        val params = FrameLayout.LayoutParams(view.layoutParams)
//        params.setMargins(
//            systemInsets.left,
//            systemInsets.top,
//            systemInsets.right,
//            systemInsets.bottom + imeHeight
//        )
//        view.layoutParams = params

        WindowInsetsCompat.CONSUMED
//        insets
    }
}

/**
 * Assigns [EditText]'s cursor color to [color]
 */
internal fun EditText.setCursorColor(@ColorInt color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return
    }
    try {
        // Get the cursor resource id
        var field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
        field.isAccessible = true
        val drawableResId = field.getInt(this)

        // Get the editor
        field = TextView::class.java.getDeclaredField("mEditor")
        field.isAccessible = true
        val editor = field.get(this)

        // Get the drawable and set a color filter
        val drawable = ContextCompat.getDrawable(this.context, drawableResId)
        drawable!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        val drawables = arrayOf(drawable, drawable)

        // Set the drawables
        field = editor.javaClass.getDeclaredField("mCursorDrawable")
        field.isAccessible = true
        field.set(editor, drawables)
    } catch (ignored: Exception) {
    }

}

/**
 * Captures focus on the [view] and shows the keyboard
 */
internal fun Fragment.showKeyboardOn(view: View, onKeyboardShown: (() -> Unit)? = null) {
    lifecycleScope.launch {
        while(!ViewCompat.isLaidOut(view))
            delay(SHOW_KEYBOARD_RETRY_DELAY_MS)
        view.requestFocus()
        (getSystemService(
            requireContext(),
            InputMethodManager::class.java
        ) as InputMethodManager).showSoftInput(view, 0)

        onKeyboardShown?.invoke()
    }
}