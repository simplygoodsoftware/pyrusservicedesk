package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.text.util.LinkifyCompat
import androidx.exifinterface.media.ExifInterface
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.view.OutlineImageView.Companion.EDGE_RIGHT
import com.pyrus.pyrusservicedesk.utils.*
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.psd_comment.view.*
import java.util.regex.Matcher
import java.util.regex.Pattern

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

    companion object {
        private const val PREVIEW_RETRY_STEP_MS = 10 * 1000L
        private const val MAX_PREVIEW_RETRY_MS = 60 * 1000L
        private val previewRatioMap = mutableMapOf<Uri, Float>()

        fun getFullSizePreviewWidth(previewUri: Uri, height: Int): Int {
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

    /**
     * Type of the content that is intended to be shown.
     * See [ContentType].
     * Assigning automatically changes the appearance of the view.
     * For example if [ContentType.Text] was assigned, then invocations of any methods that
     * are related to [ContentType.Attachment] are not give any visible effects.
     */
    var contentType: ContentType = ContentType.Text
        set(value) {
            when (value) {
                ContentType.Text -> {
                    comment_text.visibility = View.VISIBLE
                    attachment_layout.visibility = View.GONE
                    preview_layout.visibility = GONE
                }
                ContentType.Attachment -> {
                    recentProgress = 0
                    comment_text.visibility = View.GONE
                    attachment_layout.visibility = View.VISIBLE
                    preview_layout.visibility = View.GONE
                }
                ContentType.PreviewableAttachment -> {
                    recentProgress = 0
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
                    ContentType.PreviewableAttachment -> previewDownloadDrawable
                    else -> null
                }
                drawable?.let { draw ->
                    draw.setDrawableByLayerId(
                        R.id.progress_icon,
                        it.mutate().apply { setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN) })
                    draw.invalidateSelf()
                }
            }
            if (value != Status.Processing)
                setProgress(0)

            when (contentType) {
                ContentType.Attachment -> applyProgressStatusToAttachmentView(value)
                ContentType.PreviewableAttachment -> applyProgressStatusToPreview(value)
                else -> {}
            }

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

            if (value == Status.Processing)
                statusView.setColorFilter(ConfigUtils.getSecondaryColorOnMainBackground(context), PorterDuff.Mode.SRC_ATOP)
            else
                statusView.colorFilter = null

            field = value
        }


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

    private var recentPicassoTarget: Target? = null
    private var loadPreviewRunnable: Runnable? = null
    private var previewCallId = 0

    init {
        View.inflate(context, R.layout.psd_comment, this)

        type = with(getContext().obtainStyledAttributes(attrs, R.styleable.CommentView)){
            getInt(R.styleable.CommentView_type, TYPE_INBOUND).also { recycle() }
        }

        preview_passive_progress.indeterminateDrawable.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        ConfigUtils.getMainFontTypeface()?.let {
            file_name.typeface = it
            file_size.typeface = it
            comment_text.typeface = it
        }
        val backgroundColor = when (type){
            TYPE_INBOUND -> ConfigUtils.getSupportMessageTextBackgroundColor(context)
            else -> ConfigUtils.getUserMessageTextBackgroundColor(context)

        }
        primaryColor = getTextColorOnBackground(context, backgroundColor)
        val secondaryColor = adjustColorChannel(primaryColor, ColorChannel.Alpha, SECONDARY_TEXT_COLOR_MULTIPLIER)

        background_parent.setCardBackgroundColor(backgroundColor)

        comment_text.setTextColor(when(type) {
            TYPE_INBOUND -> ConfigUtils.getSupportMessageTextColor(context, backgroundColor)
            else -> ConfigUtils.getUserMessageTextColor(context, backgroundColor)
        })
        comment_text.setLinkTextColor(primaryColor)
        file_name.setTextColor(primaryColor)
        file_size.setTextColor(secondaryColor)

        fileDownloadDrawable = attachment_progress.progressDrawable as LayerDrawable
        fileDownloadDrawable.adjustSettingsForProgress(primaryColor, secondaryColor)
        previewDownloadDrawable = preview_progress.progressDrawable as LayerDrawable
        previewDownloadDrawable.adjustSettingsForProgress(primaryColor, secondaryColor)

        preview_full.outlineColor = ActivityCompat.getColor(context, R.color.psd_comment_preview_outline)
        preview_full.outlineRadius = resources.getDimensionPixelSize(R.dimen.psd_comment_radius)
        preview_full.outlineWidth = resources.getDimensionPixelSize(R.dimen.psd_comment_preview_outline_radius)

        preview_mini.outlineColor = backgroundColor
        preview_mini.outlineRadius = resources.getDimensionPixelSize(R.dimen.psd_comment_radius)
        preview_mini.outlineWidth = resources.getDimensionPixelSize(R.dimen.psd_comment_preview_outline_radius)
        preview_mini.edges = preview_mini.edges and EDGE_RIGHT.inv()

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
        comment_text.setLinkTextColor(ConfigUtils.getAccentColor(context))
        val filteredText = text.replace("<br>", "\n").replace(Regex("\\n?<button>(.*?)</button>|\\n *?$|^ *?\\n"), "")
        comment_text.text = replaceLinkTagsWithSpans(filteredText)
        LinkifyCompat.addLinks(comment_text, Linkify.WEB_URLS or Linkify.PHONE_NUMBERS)
        addDeepLinks(comment_text)
        comment_text.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun replaceLinkTagsWithSpans(text: CharSequence): CharSequence {
        val ranges = mutableListOf<Triple<String, String, IntRange>>()

        var offset = 0

        val res = text.replace(Regex("<a href=\"(.*?)\">(.*?)</a>")) { matchResult ->
            if (matchResult.groups.size < 3 || matchResult.groups[1] == null || matchResult.groups[2] == null) {
                return@replace matchResult.value
            }
            val link = matchResult.groups[1]!!.value
            val word = matchResult.groups[2]!!.value

            val visibleStart = matchResult.groups[2]!!.range.first - (matchResult.groups[2]!!.range.first - matchResult.range.first) - offset
            val visibleLength = matchResult.groups[2]!!.range.last - matchResult.groups[2]!!.range.first
            val realRange = visibleStart..visibleStart + visibleLength + 1

            ranges.add(Triple(link, word, realRange))

            offset += (matchResult.range.last - matchResult.range.first) - visibleLength

            matchResult.groups[2]!!.value
        }

        val ssb = SpannableStringBuilder(res)

        ranges.forEach { span ->
            ssb.setSpan(
                createClickableSpan(span.first, span.second),
                span.third.first,
                span.third.last,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        return ssb
    }

    fun isLinkSafe(url: String, text: String?): Boolean {
        val urlWithoutProtocol = url
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/")

        return text == url
                || ConfigUtils.getTrustedUrls()?.any { urlWithoutProtocol.startsWith(it) } == true
                || text == null
    }

    private fun createClickableSpan(url: String, text: String? = null): ClickableSpan {
        return object : ClickableSpan() {

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ConfigUtils.getAccentColor(context)
            }

            override fun onClick(widget: View) {
                if (isLinkSafe(url, text)) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
                    }
                    catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
                else {
                    showLinkDialog(context as Activity, url) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
                        }
                        catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun showLinkDialog(context: Activity, url: String, onClick: ()-> Unit) {
        val message = SpannableStringBuilder(context.getString(R.string.link_warning, url))
        val range = Regex(url).find(message)?.range
        range?.let {
            if (range.first != -1 && range.last != -1 && range.last > range.first) {
                message.setSpan(createClickableSpan(url), range.first, range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        AlertDialog.Builder(context)
            .setPositiveButton(R.string.psd_open) { _, _ -> onClick.invoke() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setMessage(message)
            .create()
            .show()
    }

    private fun addDeepLinks(textView: AppCompatTextView) {
        val ssb = SpannableStringBuilder(textView.text)
        val matcher: Matcher = Pattern.compile("(\\S+)://\\S+").matcher(ssb)

        var anyFound = false
        while (matcher.find()) {
            val group = matcher.group(1)
            if (group == "http" || group == "https") {
                continue
            }

            anyFound = true

            val clickableSpan = createClickableSpan(matcher.group())

            ssb.setSpan(clickableSpan, matcher.start(), matcher.end(), 0)
        }
        if (anyFound) {
            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.text = ssb
        }

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
            ContentType.PreviewableAttachment -> preview_progress
            else -> null
        }
        progressBar?.let {
            ObjectAnimator.ofInt(it, "progress", progress)
                .apply {
                    duration = when (progress){
                        0 -> 0
                        else -> PROGRESS_CHANGE_ANIMATION_DURATION
                    }
                    interpolator = DecelerateInterpolator()
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

    /**
     * Assigns file preview
     */
    fun setPreview(previewUri: Uri) {
        when(contentType){
            ContentType.PreviewableAttachment -> setFullSizePreview(previewUri)
            ContentType.Attachment -> setMiniPreview(previewUri)
            else -> return
        }
    }

    private fun setMiniPreview(previewUri: Uri) {
        preview_mini.setImageBitmap(null)
        preview_mini.visibility = GONE
        when {
            previewUri.isRemote() -> setNetworkPreview(previewUri, preview_mini, false)
        }
    }

    private fun setFullSizePreview(previewUri: Uri) {
        val isRemote = previewUri.isRemote()
        preview_progress.visibility = if (isRemote) GONE else View.VISIBLE
        preview_full.setImageBitmap(null)
        when {
            isRemote -> setNetworkPreview(previewUri, preview_full, true)
            else -> setLocalPreview(preview_full, previewUri, true)
        }
    }

    private fun applyProgressStatusToPreview(status: Status) {
        preview_progress.visibility = when(status){
            Status.Completed -> GONE
            else -> View.VISIBLE
        }
    }

    private fun applyProgressStatusToAttachmentView(status: Status) {
        if (status != Status.Processing)
            setFileSize(recentFileSize)
        else
            file_size.setText(R.string.psd_uploading)
    }

    private fun setNetworkPreview(previewUri: Uri,
                                  target: ImageView,
                                  adjustTargetDimensions: Boolean) {

        setNetworkPreviewDelayed(previewUri, target, adjustTargetDimensions, 0)
    }

    private fun setNetworkPreviewDelayed(previewUri: Uri,
                                         target: ImageView,
                                         adjustTargetDimensions: Boolean,
                                         delayMs: Long) {

        val allowApplyResult: (Int) -> Boolean = {
            it == previewCallId
        }
        val onFailed: (Int) -> Unit = {
            if (allowApplyResult.invoke(it)) {
                setNetworkPreviewDelayed(
                    previewUri,
                    target,
                    adjustTargetDimensions,
                    when (delayMs) {
                        0L -> PREVIEW_RETRY_STEP_MS
                        else -> Math.min(delayMs + PREVIEW_RETRY_STEP_MS, MAX_PREVIEW_RETRY_MS)
                    }
                )
            }
        }

        val picassoTarget = when{
            adjustTargetDimensions -> ChangingSizeTarget(
                target,
                previewUri,
                ++previewCallId,
                resources.getDimensionPixelSize(R.dimen.psd_recyclerview_item_height_default),
                previewRatioMap,
                allowApplyResult,
                onFailed)
            else -> SimpleTarget(target, previewUri, ++previewCallId, allowApplyResult, onFailed) { target.visibility = View.VISIBLE }
        }
        clearCurrentPreviewRequest()
        recentPicassoTarget = picassoTarget
        loadPreviewRunnable = Runnable {
            Picasso.get().load(previewUri).into(picassoTarget)
        }
        postDelayed(loadPreviewRunnable, delayMs)
    }

    private fun clearCurrentPreviewRequest() {
        loadPreviewRunnable?.let {
            removeCallbacks(it)
        }
        recentPicassoTarget?.let {
            Picasso.get().cancelRequest(it)
        }
    }

    /**
     * There is a problem with [Picasso] that doesn't allow to use [ChangingSizeTarget] with local files.
     */
    private fun setLocalPreview(target: ImageView,
                                previewUri: Uri,
                                adjustTargetDimension: Boolean,
                                onFailed: (() -> Unit)? = null) {

        val bitmap = try {
            val exif = context.contentResolver.openInputStream(previewUri)?.use {
                ExifInterface(it)
            }
            val source = exif?.thumbnailBitmap
                ?: BitmapFactory.decodeStream(context.contentResolver.openInputStream(previewUri))

            val maxByteCount = 50e6 // 50 mb
            if (source.byteCount >= maxByteCount) {
                onFailed?.invoke()
                return
            }
            with(source) {
                exif?.let {
                    this.rotate(getImageRotation(it).toFloat())
                } ?: this
            }
        } catch (ex: java.lang.Exception) {
            onFailed?.invoke()
            return
        }

        if (adjustTargetDimension) {
            val ratio  = bitmap.width.toFloat() / bitmap.height
            if (!previewRatioMap.containsKey(previewUri))
                previewRatioMap += previewUri to ratio
            target.layoutParams.width = getFullSizePreviewWidth(previewUri, target.layoutParams.height)
            target.requestLayout()
        }
        target.setImageBitmap(bitmap)
    }
}

/**
 * [hashCode] and [equals] are required by [Target] description
 */
private open class SimpleTarget(protected val targetView: ImageView,
                                protected val uri: Uri,
                                val callId: Int,
                                protected val canApplyResult: (Int) -> Boolean,
                                protected val onFailed: (Int) -> Unit,
                                protected val onSuccess: (() -> Unit)? = null)
    : Target{

    /**
     * [Picasso] stores targets in weak references, which can lead to missing of calling the callbacks as
     * particular target can be garbage collected. We handle it by storing an instance in view's tag
     */
    init {
        targetView.tag = this
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

    }

    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
        targetView.tag = null
        onFailed.invoke(callId)
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if(!canApplyResult.invoke(callId))
            return
        targetView.setImageBitmap(bitmap)
        targetView.tag = null
        onSuccess?.invoke()
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
}

private class ChangingSizeTarget(targetView: ImageView,
                                 uri: Uri,
                                 callId: Int,
                                 private val minWidth: Int,
                                 val ratioMap: MutableMap<Uri, Float>,
                                 canApplyResult: (Int) -> Boolean,
                                 onFailed: (Int) -> Unit)
    : SimpleTarget(targetView, uri, callId, canApplyResult, onFailed) {

    private companion object {
        const val CHANGING_SIZE_ANIMATION_DURATION_MS = 150L
    }

    init {
        targetView.layoutParams.width = Math.max(
            CommentView.getFullSizePreviewWidth(uri, targetView.layoutParams.height),
            minWidth)
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
            targetView.layoutParams.width = Math.max(
                (targetView.layoutParams.height * it.animatedValue as Float).toInt(),
                minWidth)
            targetView.requestLayout()
        }
        fun onAnimationEnd() {
            super.onBitmapLoaded(bitmap, from)
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) = onAnimationEnd()
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
    PreviewableAttachment
}