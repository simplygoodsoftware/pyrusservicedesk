package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.text.util.LinkifyCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.AppCompatImageView
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.utils.*
import com.pyrus.pyrusservicedesk.utils.ConfigUtils.Companion.getAccentColor
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
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
                fileDownloadDrawable.setDrawableByLayerId(
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
     * Progress icon can be completely hidden by switching this value to FALSE
     */
    var isFileProgressVisible = true
        set(value) {
            attachment_progress.visibility = if (value) View.VISIBLE else View.GONE
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
    private val type: Int
    private val statusView:AppCompatImageView

    init {
        View.inflate(context, R.layout.psd_comment, this)

        type = with(getContext().obtainStyledAttributes(attrs, R.styleable.CommentView)){
            getInt(R.styleable.CommentView_type, TYPE_INBOUND).also { recycle() }
        }

        val backgroundColor = when (type){
            TYPE_INBOUND -> ContextCompat.getColor(context, R.color.psd_comment_inbound_background)
            else -> getAccentColor(context)
        }
        primaryColor = getTextColorOnBackground(context, backgroundColor)
        val secondaryColor = adjustColorChannel(primaryColor, ColorChannel.Alpha, SECONDARY_TEXT_COLOR_MULTIPLIER)

        background_parent.background.mutate().setColorFilter(backgroundColor, PorterDuff.Mode.SRC_IN)
        comment_text.setTextColor(primaryColor)
        comment_text.setLinkTextColor(primaryColor)
        root.gravity = Gravity.BOTTOM or when(type){
            TYPE_INBOUND -> Gravity.START
            else -> Gravity.END
        }
        file_name.setTextColor(primaryColor)
        file_size.setTextColor(secondaryColor)

        fileDownloadDrawable = (attachment_progress.progressDrawable as LayerDrawable).apply {
            findDrawableByLayerId(android.R.id.background)
                    .mutate()
                    .setColorFilter(
                        adjustColorChannel(
                            secondaryColor,
                            ColorChannel.Alpha,
                            PROGRESS_BACKGROUND_MULTIPLIER),
                        PorterDuff.Mode.SRC_IN)

            findDrawableByLayerId(android.R.id.progress)
                    .mutate()
                    .setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            findDrawableByLayerId(R.id.progress_icon)
                    .mutate()
                    .setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
        }
        attachment_progress.setOnClickListener { onDownloadIconClickListener?.invoke() }
        statusView = AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                    resources.getDimension(R.dimen.psd_comment_error_width).toInt(),
                    LayoutParams.WRAP_CONTENT)
                    .apply { gravity = Gravity.CENTER_VERTICAL }
        }
        status = Status.Completed
        isFileProgressVisible = true
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
            val margin = resources.getDimension(R.dimen.psd_comment_error_width_negative).toInt()
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
        ValueAnimator.ofInt(attachment_progress.progress, progress).apply {
            duration = when (progress){
                0 -> 0
                else -> PROGRESS_CHANGE_ANIMATION_DURATION
            }
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                (animatedValue as Int).let { value ->
                    if (recentProgress > progress)
                        cancel()
                    attachment_progress.progress = value
                }
            }
        }.also{ it.start() }
    }

    /**
     * Assigns [listener] that is invoked when progress icon was clicked by user.
     */
    fun setOnProgressIconClickListener(listener: () -> Unit) {
        onDownloadIconClickListener = listener
    }

    fun setPreview(previewUri: Uri, isLocal: Boolean) {
        when {
            isLocal -> preview_progress.visibility = View.GONE
            else -> {
                preview_progress.visibility = View.VISIBLE
                preview_progress.progress = 40
            }
        }
        Picasso.get()
            .load(previewUri)
            .noFade() // fade breaks image appearance when loaded
            .into(
                preview,
                object : Callback{
                    override fun onSuccess() {
                        preview_progress.progress = 100
                        preview_progress.visibility = View.GONE
                    }

                    override fun onError(e: Exception?) {

                    }

                })
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