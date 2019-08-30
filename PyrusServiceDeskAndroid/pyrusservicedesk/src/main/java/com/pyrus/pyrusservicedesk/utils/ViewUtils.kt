package com.pyrus.pyrusservicedesk.utils

import android.graphics.PorterDuff
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.TextView

/**
 * Checks whether given recyclerview is at end.
 *
 * NB: applicable to recyclerviews that have [LinearLayoutManager] only
 */
internal fun androidx.recyclerview.widget.RecyclerView.isAtEnd(): Boolean {
    return adapter == null
            || layoutManager == null
            || childCount == 0
            || (layoutManager as androidx.recyclerview.widget.LinearLayoutManager).findLastVisibleItemPosition() == adapter!!.itemCount - 1
}

/**
 * Assigns [EditText]'s cursor color to [color]
 */
internal fun EditText.setCursorColor(@ColorInt color: Int) {
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