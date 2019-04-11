package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.utils.RoundedCornerTransformation


internal class RoundCornersLayout @JvmOverloads constructor(context: Context,
                                                            attrs: AttributeSet? = null,
                                                            defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val transformation = RoundedCornerTransformation(resources.getDimension(R.dimen.psd_comment_radius))

    override fun draw(canvas: Canvas?) {
        if (canvas == null || width == 0 || height == 0)
            return
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        super.draw(Canvas(output))
        val withRoundedCorners = transformation.transform(output)
        canvas.drawBitmap(
            withRoundedCorners,
            0f,
            0f,
            Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            })
    }


}