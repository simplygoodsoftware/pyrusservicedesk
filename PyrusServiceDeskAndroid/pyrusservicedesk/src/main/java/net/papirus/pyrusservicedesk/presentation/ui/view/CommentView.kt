package net.papirus.pyrusservicedesk.presentation.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.LayerDrawable
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_comment.view.*
import net.papirus.pyrusservicedesk.utils.*
import net.papirus.pyrusservicedesk.utils.ConfigUtils.Companion.getAccentColor

private const val TYPE_INBOUND = 0
private const val TYPE_OUTBOUND = 1

private const val PROGRESS_BACKGROUND_MULTIPLIER = 0.3f
private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
private const val PROGRESS_CHANGE_ANIMATION_DURATION = 100L

internal class CommentView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    var contentType: ContentType = ContentType.Text
        set(value) {
            when (value) {
                ContentType.Text -> {
                    comment_text.visibility = View.VISIBLE
                    attachment_layout.visibility = View.GONE
                }
                ContentType.Attachment ->{
                    recentProgress = 0
                    comment_text.visibility = View.GONE
                    attachment_layout.visibility = View.VISIBLE
                }
            }
            field = value
        }

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

    var isFileProgressVisible = true
        set(value) {
            file_progress.visibility = if (value) View.VISIBLE else View.GONE
            field = value
        }

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
        val secondaryColor = adjustColor(primaryColor, ColorChannel.Alpha, SECONDARY_TEXT_COLOR_MULTIPLIER)

        background_parent.background.mutate().setColorFilter(backgroundColor, PorterDuff.Mode.SRC)
        comment_text.setTextColor(primaryColor)
        root.gravity = Gravity.BOTTOM or if (type == TYPE_INBOUND) Gravity.START else Gravity.END
        file_name.setTextColor(primaryColor)
        file_size.setTextColor(secondaryColor)

        fileDownloadDrawable = (file_progress.progressDrawable as LayerDrawable).apply {
            findDrawableByLayerId(android.R.id.background)
                    .mutate()
                    .setColorFilter(
                        adjustColor(
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
        file_progress.setOnClickListener { onDownloadIconClickListener?.invoke() }
        statusView = AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                    resources.getDimension(R.dimen.psd_comment_error_width).toInt(),
                    LayoutParams.WRAP_CONTENT)
                    .apply { gravity = Gravity.CENTER_VERTICAL }
        }
        status = Status.Completed
        isFileProgressVisible = true
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

    fun setCommentText(text: String) {
        comment_text.text = text
    }

    fun setFileName(fileName: String) {
        file_name.text = fileName
    }

    fun setFileSize(bytesSize: Float) {
        recentFileSize = bytesSize
        val isMegabytes = recentFileSize >= BYTES_IN_MEGABYTE / 10
        val toShow = when {
            isMegabytes -> recentFileSize / BYTES_IN_MEGABYTE
            else -> recentFileSize / BYTES_IN_KILOBYTE
        }
        file_size.text = context.getString(if (isMegabytes) R.string.psd_file_size_mb else R.string.psd_file_size_kb, toShow)
    }

    fun setProgress(progress: Int) {
        val currentProgress = recentProgress
        recentProgress = progress
        ValueAnimator.ofInt(file_progress.progress, progress).apply {
            duration = if (currentProgress == 0) PROGRESS_CHANGE_ANIMATION_DURATION else 0
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                (animatedValue as Int).let { value ->
                    if (recentProgress > progress)
                        cancel()
                    file_progress.progress = value
                }
            }
        }.also{ it.start() }
    }

    fun setOnProgressIconClickListener(listener: () -> Unit) {
        onDownloadIconClickListener = listener
    }
}

internal enum class Status {
    Processing,
    Completed,
    Error
}

internal enum class ContentType {
    Text,
    Attachment
}