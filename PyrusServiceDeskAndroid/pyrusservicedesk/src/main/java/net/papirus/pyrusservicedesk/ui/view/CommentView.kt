package net.papirus.pyrusservicedesk.ui.view

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.support.annotation.ColorInt
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_comment.view.*
import net.papirus.pyrusservicedesk.utils.*

private const val TYPE_INBOUND = 0
private const val TYPE_OUTBOUND = 1

private const val DOWNLOAD_PROGRESS_BACKGROUND_MULTIPLIER = 0.3f

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
                    comment_text.visibility = View.GONE
                    attachment_layout.visibility = View.VISIBLE
                }
            }
            field = value
        }

    var fileDownloadingState = DownloadingState.Remote
        set(value) {
            val icon = ActivityCompat.getDrawable(
                    context,
                    when (value) {
                        DownloadingState.Remote -> R.drawable.psd_download_file
                        DownloadingState.Downloading -> R.drawable.psd_close
                        DownloadingState.Error -> R.drawable.psd_refresh
                        else -> R.drawable.psd_arrow_back
                    })
            icon?.let {
                fileDownloadDrawable.setDrawableByLayerId(
                        R.id.progress_icon,
                        it.mutate().apply { setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN) })
            }
            if (value != DownloadingState.Downloading) {
                setFileSize(recentFileSize)
                setDownloadingProgress(0)
            }
            else
                file_size.setText(R.string.psd_downloading)
            field = value
        }

    var isErrorVisible: Boolean = false
        set(value) {
            errorView.visibility = if (value) View.VISIBLE else View.INVISIBLE
            field = value
        }

    private var onDownloadIconClickListener: (() -> Unit)? = null
    private var recentFileSize: Float = 0f
    @ColorInt
    private val primaryColor: Int
    private val fileDownloadDrawable: LayerDrawable
    private val type: Int
    private val errorView:AppCompatImageView

    init {
        View.inflate(context, R.layout.psd_comment, this)

        type = with(getContext().obtainStyledAttributes(attrs, R.styleable.CommentView)){
            getInt(R.styleable.CommentView_type, TYPE_INBOUND).also { recycle() }
        }

        val backgroundColor = ContextCompat.getColor(
                context,
                when (type){
                    TYPE_INBOUND -> R.color.psd_comment_inbound_background
                    else -> R.color.psd_accent
                })
        primaryColor = getColor(
                context,
                when (type){
                    TYPE_INBOUND -> android.R.attr.textColorPrimary
                    else -> android.R.attr.textColorPrimaryInverse
                })
        val secondaryColor = getColor(
                context,
                when(type){
                    TYPE_INBOUND -> android.R.attr.textColorSecondary
                    else -> android.R.attr.textColorSecondaryInverse

                })

        background_parent.background.mutate().setColorFilter(backgroundColor, PorterDuff.Mode.SRC)
        comment_text.setTextColor(primaryColor)
        root.gravity = Gravity.BOTTOM or if (type == TYPE_INBOUND) Gravity.START else Gravity.END
        file_name.setTextColor(primaryColor)
        file_size.setTextColor(secondaryColor)

        fileDownloadDrawable = (file_downloading_progress.progressDrawable as LayerDrawable).apply {
            findDrawableByLayerId(android.R.id.background)
                    .mutate()
                    .setColorFilter(
                        when (type){
                            TYPE_INBOUND -> adjustColor(
                                secondaryColor,
                                ColorChannel.Alpha,
                                DOWNLOAD_PROGRESS_BACKGROUND_MULTIPLIER)
                            else -> secondaryColor
                        },
                        PorterDuff.Mode.SRC_IN)

            findDrawableByLayerId(android.R.id.progress)
                    .mutate()
                    .setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            findDrawableByLayerId(R.id.progress_icon)
                    .mutate()
                    .setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
        }
        file_downloading_progress.setOnClickListener { onDownloadIconClickListener?.invoke() }
        errorView = AppCompatImageView(context).apply {
            setImageResource(R.drawable.psd_error)
            layoutParams = LinearLayout.LayoutParams(
                    resources.getDimension(R.dimen.psd_comment_error_width).toInt(),
                    LayoutParams.WRAP_CONTENT)
                    .apply { gravity = Gravity.CENTER_VERTICAL }
        }
        isErrorVisible = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        root.removeView(errorView)
        (layoutParams as MarginLayoutParams).apply {
            val margin = resources.getDimension(R.dimen.psd_comment_error_width_negative).toInt()
            when (type) {
                TYPE_INBOUND -> {
                    rightMargin = margin
                    root.addView(errorView)
                }
                else -> {
                    leftMargin = margin
                    root.addView(errorView, 0)
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
        val toShow = when {
            recentFileSize >= BYTES_IN_MEGABYTE / 10 -> recentFileSize / BYTES_IN_MEGABYTE
            else -> recentFileSize / BYTES_IN_KILOBYTE
        }
        file_size.text = context.getString(R.string.psd_file_size, toShow)
    }

    fun setDownloadingProgress(progress: Int) {
        file_downloading_progress.progress = progress
    }

    fun setOnDownloadIconClickListener(listener: () -> Unit) {
        onDownloadIconClickListener = listener
    }
}

internal enum class ContentType {
    Text,
    Attachment
}

internal enum class DownloadingState{
    Remote,
    Downloading,
    Downloaded,
    Error
}