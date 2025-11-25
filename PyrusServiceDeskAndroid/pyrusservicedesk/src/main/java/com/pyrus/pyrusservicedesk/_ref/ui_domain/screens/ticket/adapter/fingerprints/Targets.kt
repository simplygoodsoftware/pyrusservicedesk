package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.widget.ImageView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.Targets.Companion.getFullSizePreviewWidth
import com.pyrus.pyrusservicedesk._ref.utils.ColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.adjustColorChannel
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class Targets {
    companion object {

        private const val PROGRESS_BACKGROUND_MULTIPLIER = 0.3f
        private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
        private const val PROGRESS_CHANGE_ANIMATION_DURATION = 100L

        fun getFullSizePreviewWidth(previewUri: Uri, height: Int, previewRatioMap:  MutableMap<Uri, Float>): Int {
            return when{
                previewRatioMap.containsKey(previewUri) -> (height * (previewRatioMap[previewUri] ?: 1f)).toInt()
                else -> height
            }
        }

        private fun LayerDrawable.adjustSettingsForProgress(primaryColor: Int, secondaryColor: Int) {
            findDrawableByLayerId(android.R.id.background)
                .mutate()
                .setColorFilter(
                    adjustColorChannel(
                        secondaryColor,
                        ColorChannel.Alpha,
                        PROGRESS_BACKGROUND_MULTIPLIER
                    ),
                    PorterDuff.Mode.SRC_IN
                )

            findDrawableByLayerId(android.R.id.progress)
                .mutate()
                .setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            findDrawableByLayerId(R.id.progress_icon)
                .mutate()
                .setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
        }
    }
}

/**
 * [hashCode] and [equals] are required by [Target] description
 */
open class SimpleTarget
    (
    protected val targetView: ImageView,
    protected val uri: Uri,
    val callId: Int,
    private val lifecycleScope: CoroutineScope,
    private val retryDelay: Long = 1000L,
    protected val canApplyResult: (Int) -> Boolean,
    protected val onSuccess: (() -> Unit)? = null,
) : Target {

    private var currentJob: Job? = null
    private var delayMs: Long = 0L
    private var retryCount = 0

    /**
     * [Picasso] stores targets in weak references, which can lead to missing of calling the callbacks as
     * particular target can be garbage collected. We handle it by storing an instance in view's tag
     */
    init {
        targetView.tag = this
        delayMs = retryDelay
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

    }

    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
        targetView.tag = null
        cancel()
        if (canApplyResult.invoke(callId) && retryCount++ < 5) {
            currentJob = lifecycleScope.launch {
                delayMs = if (delayMs == 0L) PREVIEW_RETRY_STEP_MS
                else min(delayMs + PREVIEW_RETRY_STEP_MS, MAX_PREVIEW_RETRY_MS)
                delay(retryDelay)
                PyrusServiceDesk.injector().picasso.load(uri).into(this@SimpleTarget)
            }
        }

    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if (!canApplyResult.invoke(callId))
            return
        targetView.setImageBitmap(bitmap)
        targetView.tag = null
        currentJob?.cancel()
        onSuccess?.invoke()
    }

    fun cancel() {
        currentJob?.cancel()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other == null || other.javaClass != this.javaClass -> false
            else -> uri == (other as SimpleTarget).uri
        }
    }

    override fun hashCode(): Int {
        return uri.hashCode()
    }

    companion object {
        private const val PREVIEW_RETRY_STEP_MS = 10 * 1000L
        private const val MAX_PREVIEW_RETRY_MS = 60 * 1000L
    }
}

class ChangingSizeTarget(
    targetView: ImageView,
    uri: Uri,
    callId: Int,
    private val minWidth: Int,
    lifecycleScope: CoroutineScope,
    retryDelay: Long = 1000L,
    val ratioMap: MutableMap<Uri, Float>,
    canApplyResult: (Int) -> Boolean,
) : SimpleTarget(targetView, uri, callId, lifecycleScope, retryDelay, canApplyResult) {

    private companion object {
        const val CHANGING_SIZE_ANIMATION_DURATION_MS = 150L
    }

    init {
        targetView.layoutParams.width = max(
            getFullSizePreviewWidth(uri, targetView.layoutParams.height, ratioMap),
            minWidth
        )
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if (!canApplyResult.invoke(callId))
            return
        if (bitmap == null || ratioMap.containsKey(uri)) {
            super.onBitmapLoaded(bitmap, from)
            return
        }

        val ratio = bitmap.width.toFloat() / bitmap.height
        ratioMap += uri to ratio
        val animator = ObjectAnimator.ofFloat(1f, ratio)
        animator.duration = CHANGING_SIZE_ANIMATION_DURATION_MS
        animator.addUpdateListener {
            targetView.layoutParams.width = max(
                (targetView.layoutParams.height * it.animatedValue as Float).toInt(),
                minWidth
            )
            targetView.requestLayout()
        }
        fun onAnimationEnd() {
            super.onBitmapLoaded(bitmap, from)
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {}
            override fun onAnimationEnd(p0: Animator) { onAnimationEnd() }
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationRepeat(p0: Animator) {}
        })
        animator.start()
    }
}

/**
 * Used for definition of status of the comment overall by [CommentView.status] or
 * status of file uploading/downloading by [CommentView.fileProgressStatus]
 */
enum class Status {
    Processing,
    Completed,
    Error
}

/**
 * Used for definition of status of the comment overall by [CommentView.status] or
 * status of file uploading/downloading by [CommentView.fileProgressStatus]
 */
enum class AudioStatus {
    Processing,
    Error,
    Playing,
    Paused,
    None,
}


/**
 * Used for definition of the content type of [CommentView]
 */
internal enum class ContentType {
    Text,
    Attachment,
    PreviewableAttachment,
    AudioAttachment,
}