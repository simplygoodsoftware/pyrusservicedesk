package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.support.annotation.ColorInt
import android.support.media.ExifInterface
import android.support.v4.content.ContextCompat
import android.support.v4.text.util.LinkifyCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.AppCompatImageView
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.utils.*
import com.pyrus.pyrusservicedesk.utils.ConfigUtils.Companion.getAccentColor
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.psd_comment.view.*

private const val TYPE_INBOUND = 0
private const val TYPE_OUTBOUND = 1

private const val PROGRESS_BACKGROUND_MULTIPLIER = 0.3f
private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
private const val PROGRESS_CHANGE_ANIMATION_DURATION = 100L

/**
 * View that is used for rendering comment.
 * Can can be it one of the [ContentType]. [ContentType.Text] is used by default.
 * Switching the [contentType] automatically changes the appearance of the view.
 *
 * Also appearance depends on the [R.styleable.CommentView_type] that can be specified in
 * xml declaration of the view. By default [TYPE_INBOUND] is used.
 */
internal class CommentView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private companion object {
        val ratioMap = mutableMapOf<Uri, Float>()

        fun getFullSizePreviewWidth(previewUri: Uri, height: Int): Int {
            return when{
                ratioMap.containsKey(previewUri) -> (height * (ratioMap[previewUri] ?: 1f)).toInt()
                else -> height
            }
        }

        fun LayerDrawable.adjustSettingsForProgress(primaryColor: Int, secondaryColor: Int) {
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

    /**
     * Type of the content that is intended to be shown.
     * See [ContentType].
     * Assigning automatically changes the appearance of the view.
     * For example if [ContentType.Text] was assigned, then invocations of any methods that
     * are related to [ContentType.Attachment] are not give any visible effects.
     *
     */
    var contentType: ContentType = ContentType.Text
        set(value) {
            when (value) {
                ContentType.Text -> {
                    background_parent.setPadding(backgroundPadding, backgroundPadding, backgroundPadding, backgroundPadding)
                    comment_text.visibility = View.VISIBLE
                    attachment_layout.visibility = View.GONE
                    preview_layout.visibility = GONE
                }
                ContentType.Attachment -> {
                    recentProgress = 0
                    background_parent.setPadding(backgroundPadding, backgroundPadding, backgroundPadding, backgroundPadding)
                    comment_text.visibility = View.GONE
                    attachment_layout.visibility = View.VISIBLE
                    preview_layout.visibility = View.GONE
                }
                ContentType.AttachmentFullSize -> {
                    recentProgress = 0
                    background_parent.setPadding(0, 0, 0, 0)
                    comment_text.visibility = GONE
                    attachment_layout.visibility = GONE
                    preview_layout.visibility = View.VISIBLE
                }
            }
            field = value
        }

    /**
     * Change this if status of downloading/uploading status was changed.
     * This causes change of the appearance of the progress icon.
     */
    var fileProgressStatus = Status.Completed
        set(value) {
            val icon = AppCompatResources.getDrawable(
                    context,
                    when (value) {
                        Status.Processing -> R.drawable.psd_close
                        Status.Error -> R.drawable.psd_refresh
                        else -> R.drawable.psd_download_file
                    })
            icon?.let {
                val drawable = when (contentType) {
                    ContentType.Attachment -> fileDownloadDrawable
                    ContentType.AttachmentFullSize -> previewDownloadDrawable
                    else -> null
                }
                drawable?.setDrawableByLayerId(
                        R.id.progress_icon,
                        it.mutate().apply { setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN) })
            }
            if (value != Status.Processing) {
                setFileSize(recentFileSize)
                setProgress(0)
            }
            else
                file_size.setText(R.string.psd_uploading)
            field = value
        }

    /**
     * Overall comment state.
     * User for rendering of the comments states, like indicating progress and errors.
     */
    var status = Status.Completed
        set(value) {
            var visibility = View.VISIBLE
            var iconResId: Int? = null
            when (value) {
                Status.Error -> iconResId = R.drawable.psd_error
                Status.Processing -> iconResId = R.drawable.psd_sync_clock
                Status.Completed -> visibility = INVISIBLE
            }
            statusView.visibility = visibility
            iconResId?.let {
                statusView.setImageResource(it)
                if (value == Status.Processing)
                    (statusView.drawable as AnimationDrawable).start()
            }

            field = value
        }

    private var backgroundPadding = resources.getDimensionPixelSize(R.dimen.psd_comment_radius)

    private var onDownloadIconClickListener: (() -> Unit)? = null
    private var recentFileSize: Float = 0f
    private var recentProgress: Int = 0
    @ColorInt
    private val primaryColor: Int
    private val fileDownloadDrawable: LayerDrawable
    private val previewDownloadDrawable: LayerDrawable
    private val type: Int
    private val statusView:AppCompatImageView

    private val progressClickListener = OnClickListener { onDownloadIconClickListener?.invoke() }

    init {
        View.inflate(context, R.layout.psd_comment, this)

        type = with(getContext().obtainStyledAttributes(attrs, R.styleable.CommentView)){
            getInt(R.styleable.CommentView_type, TYPE_INBOUND).also { recycle() }
        }

        preview_passive_progress.indeterminateDrawable.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        val backgroundColor = when (type){
            TYPE_INBOUND -> ContextCompat.getColor(context, R.color.psd_comment_inbound_background)
            else -> getAccentColor(context)
        }
        primaryColor = getTextColorOnBackground(context, backgroundColor)
        val secondaryColor = adjustColorChannel(primaryColor, ColorChannel.Alpha, SECONDARY_TEXT_COLOR_MULTIPLIER)

        background_parent.background.mutate().setColorFilter(backgroundColor, PorterDuff.Mode.SRC_IN)

        comment_text.setTextColor(primaryColor)
        comment_text.setLinkTextColor(primaryColor)
        file_name.setTextColor(primaryColor)
        file_size.setTextColor(secondaryColor)

        fileDownloadDrawable = attachment_progress.progressDrawable as LayerDrawable
        fileDownloadDrawable.adjustSettingsForProgress(primaryColor, secondaryColor)
        previewDownloadDrawable = preview_progress.progressDrawable as LayerDrawable
        previewDownloadDrawable.adjustSettingsForProgress(primaryColor, secondaryColor)

        root.gravity = Gravity.BOTTOM or when(type){
            TYPE_INBOUND -> Gravity.START
            else -> Gravity.END
        }

        preview_progress.setOnClickListener(progressClickListener)
        attachment_progress.setOnClickListener(progressClickListener)
        statusView = AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                    resources.getDimension(R.dimen.psd_comment_error_width).toInt(),
                    LayoutParams.WRAP_CONTENT)
                    .apply { gravity = Gravity.CENTER_VERTICAL }
        }
        status = Status.Completed
    }

    override fun setOnClickListener(l: OnClickListener?) {
        background_parent.setOnClickListener(l)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        background_parent.setOnLongClickListener(l)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        root.removeView(statusView)
        (layoutParams as MarginLayoutParams).apply {
            val margin = -statusView.layoutParams.width
            when (type) {
                TYPE_INBOUND -> {
                    rightMargin = margin
                    root.addView(statusView)
                }
                else -> {
                    leftMargin = margin
                    root.addView(statusView, 0)
                }
            }
        }
    }

    /**
     * Assigns [text] of the comments.
     * Works with [ContentType.Text].
     */
    fun setCommentText(text: String) {
        comment_text.text = text
        LinkifyCompat.addLinks(comment_text, Linkify.WEB_URLS or Linkify.PHONE_NUMBERS)
    }

    /**
     * Assigns [fileName] of the file to be shown.
     * Works with [ContentType.Attachment].
     */
    fun setFileName(fileName: String) {
        file_name.text = fileName
    }

    /**
     * Uses [bytesSize] to render the size of the file.
     * Works with [ContentType.Attachment].
     */
    fun setFileSize(bytesSize: Float) {
        recentFileSize = bytesSize
        val isMegabytes = recentFileSize >= BYTES_IN_MEGABYTE / 10
        val toShow = when {
            isMegabytes -> recentFileSize / BYTES_IN_MEGABYTE
            else -> recentFileSize / BYTES_IN_KILOBYTE
        }
        val textResId = when{
            isMegabytes -> R.string.psd_file_size_mb
            else -> R.string.psd_file_size_kb
        }
        file_size.text = context.getString(textResId, toShow)
    }

    /**
     * Assigns [progress] that should shown on progress icon.
     * Works with [ContentType.Attachment].
     */
    fun setProgress(progress: Int) {
        recentProgress = progress
        val progressBar = when(contentType){
            ContentType.Attachment -> attachment_progress
            ContentType.AttachmentFullSize -> preview_progress
            else -> null
        }
        progressBar?.let { progressbar ->
            ValueAnimator.ofInt(progressbar.progress, progress)
                .apply {
                    duration = when (progress){
                        0 -> 0
                        else -> PROGRESS_CHANGE_ANIMATION_DURATION
                    }
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        (animatedValue as Int).let { value ->
                            if (recentProgress > progress)
                                cancel()
                            progressbar.progress = value
                        }
                    }
                }
                .start()
        }
    }

    /**
     * Assigns [listener] that is invoked when progress icon was clicked by user.
     */
    fun setOnProgressIconClickListener(listener: () -> Unit) {
        onDownloadIconClickListener = listener
    }

    fun setPreview(previewUri: Uri, isLocal: Boolean) {
        preview_progress.visibility = if (isLocal) View.VISIBLE else GONE
        preview.setImageBitmap(null)
        when {
            isLocal -> setLocalPreview(previewUri)
            else -> setNetworkPreview(previewUri)
        }
    }

    private fun setNetworkPreview(previewUri: Uri) {
        preview.layoutParams.width = getFullSizePreviewWidth(previewUri, preview.layoutParams.height)
        Picasso.get()
            .load(previewUri)
            .noFade() // fade breaks image appearance when loaded
            .into(ChangingSizeTarget(preview, previewUri, ratioMap))
    }

    /**
     * There is a problem with [Picasso] that doesn't allow to use [ChangingSizeTarget] with local files.
     */
    private fun setLocalPreview(previewUri: Uri) {
        val exif = ExifInterface(context.contentResolver.openInputStream(previewUri))
        val bitmap = (exif.thumbnailBitmap
            ?: BitmapFactory.decodeStream(context.contentResolver.openInputStream(previewUri)))
            .also {
                it.rotate(getImageRotation(exif).toFloat())
            }
        val ratio  = bitmap.width.toFloat() / bitmap.height
        if (!ratioMap.containsKey(previewUri))
            ratioMap += previewUri to ratio
        preview.layoutParams.width = getFullSizePreviewWidth(previewUri, preview.layoutParams.height)
        preview.setImageBitmap(bitmap)
    }
}

private class ChangingSizeTarget(val targetView: ImageView, val uri: Uri, val ratioMap: MutableMap<Uri, Float>) : Target {
    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        Log.d("", "")
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if (bitmap == null)
            return
        if (ratioMap.containsKey(uri)) {
            targetView.setImageBitmap(bitmap)
            return
        }
        val ratio = bitmap.width.toFloat() / bitmap.height
        ratioMap += uri to ratio
        val animator = ObjectAnimator.ofFloat(1f, ratio)
        animator.duration = 100L
        animator.addUpdateListener {
            targetView.layoutParams.width = (targetView.layoutParams.height * it.animatedValue as Float).toInt()
            targetView.requestLayout()
        }
        animator.addListener(object: Animator.AnimatorListener{
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) = targetView.setImageBitmap(bitmap)
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}

        })
        animator.start()
    }
}

/**
 * Used for definition of status of the comment overall by [CommentView.status] or
 * status of file uploading/downloading by [CommentView.fileProgressStatus]
 */
internal enum class Status {
    Processing,
    Completed,
    Error
}

/**
 * Used for definition of the content type of [CommentView]
 */
internal enum class ContentType {
    Text,
    Attachment,
    AttachmentFullSize
}