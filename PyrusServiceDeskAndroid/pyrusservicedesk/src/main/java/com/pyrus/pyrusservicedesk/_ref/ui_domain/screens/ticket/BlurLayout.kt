package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.renderscript.Allocation
import android.renderscript.Element.U8_4
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation

class BlurLayout: FrameLayout {
    private var canvas: Canvas? = null
    private var bitmap: Bitmap? = null
    private var parent: ViewGroup? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        init(measuredWidth, measuredHeight)
    }

    private fun init(measuredWidth: Int, measuredHeight: Int) {
        if (measuredWidth <= 0 || measuredHeight <= 0) {
            bitmap = null
            canvas = null
        }
        val bitmap = createBitmap(measuredWidth, measuredHeight)
        this.bitmap = bitmap
        canvas = Canvas(bitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bitmap = bitmap ?: return
        if (bitmap.height <= 0 || bitmap.width <= 0) return

        canvas.withSave {
            drawBitmap(bitmap, 0f, 0f, null)
        }
    }

    private fun getBackgroundAndDrawBehind() {
        //Arrays to store the co-ordinates
        val rootLocation = IntArray(2)
        val viewLocation = IntArray(2)

        parent?.getLocationOnScreen(rootLocation) //get the parent co-ordinates
        this.getLocationOnScreen(viewLocation) //get view co-ordinates

        //Calculate relative co-ordinates
        val left: Int = viewLocation[0] - rootLocation[0]
        val top: Int = viewLocation[1] - rootLocation[1]

        canvas?.withTranslation(-left.toFloat(), -top.toFloat()) {
            parent?.draw(this)
        }
        blurWithRenderScript()
    }

    private fun blurWithRenderScript() {
        val bitmap = bitmap ?: return
        if (bitmap.height <= 0 || bitmap.width <= 0) return


        val renderScript = RenderScript.create(context)
        val blurScript = ScriptIntrinsicBlur.create(renderScript, U8_4(renderScript))

        val inAllocation = Allocation.createFromBitmap(renderScript, bitmap)
        val outAllocation = Allocation.createTyped(renderScript, inAllocation.type)
        blurScript.setRadius(25f)
        blurScript.setInput(inAllocation)

        blurScript.forEach(outAllocation)
        outAllocation.copyTo(bitmap)

        inAllocation.destroy()
    }

    fun setParent(parent: ViewGroup) {
        cleanParent()
        this.parent = parent
        this.parent?.viewTreeObserver?.addOnPreDrawListener(drawListener)
    }

    fun cleanParent() {
        this.parent?.viewTreeObserver?.removeOnPreDrawListener(drawListener)
    }

    private val drawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            getBackgroundAndDrawBehind()
            return true
        }
    }

}