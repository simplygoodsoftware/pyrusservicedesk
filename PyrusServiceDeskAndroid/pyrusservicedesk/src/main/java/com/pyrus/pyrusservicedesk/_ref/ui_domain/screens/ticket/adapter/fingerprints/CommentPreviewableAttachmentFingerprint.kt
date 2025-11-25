package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.animation.ObjectAnimator
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout.GONE
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.Targets.Companion.getFullSizePreviewWidth
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.adjustColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.getImageRotation
import com.pyrus.pyrusservicedesk._ref.utils.getTextColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.isRemote
import com.pyrus.pyrusservicedesk._ref.utils.rotate
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentPreviewableAttachmentBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import com.squareup.picasso.Picasso
import kotlin.reflect.KClass

internal class CommentPreviewableAttachmentFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
    private val lifecycleOwner: LifecycleOwner,
) : ItemFingerprint<CommentEntry.Comment.CommentPreviewableAttachment>() {
    override val layoutId: Int = R.layout.psd_view_holder_comment_previewable_attachment

    override val entryKeyKClass: KClass<*> =
        CommentEntry.Comment.CommentPreviewableAttachment::class


    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.Comment.CommentPreviewableAttachment> =
        CommentPreviewableAttachmentHolder(
            PsdViewHolderCommentPreviewableAttachmentBinding.inflate(layoutInflater, parent, false),
            lifecycleOwner,
            onEvent
        )

    override fun areItemsTheSame(
        oldItem: CommentEntry.Comment.CommentPreviewableAttachment,
        newItem: CommentEntry.Comment.CommentPreviewableAttachment
    ) = newItem.entryId == oldItem.entryId
}

internal class CommentPreviewableAttachmentHolder(
    val binding: PsdViewHolderCommentPreviewableAttachmentBinding,
    private val lifecycleOwner: LifecycleOwner,
    onEvent: (event: TicketView.Event) -> Unit,
) : BaseViewHolder<CommentEntry.Comment.CommentPreviewableAttachment>(binding.root) {

    private var hasError: Boolean = false
    private var id: Long = -1
    private var attachId: Long = -1
    private var previewCallId = 0

    private var recentPicassoTarget: SimpleTarget? = null
    private var previewDownloadDrawable: LayerDrawable

    @ColorInt
    private var primaryColor: Int = 0


    private val onCommentClickListener = View.OnClickListener {
        when {
            hasError -> onEvent(TicketView.Event.OnErrorCommentClick(id))
            fileProgressStatus == Status.Completed ->
                onEvent(TicketView.Event.OnPreviewClick(id, attachId))
        }
    }

    init {
        binding.comment.previewPassiveProgress.indeterminateDrawable.setColorFilter(
            Color.BLACK,
            PorterDuff.Mode.SRC_IN
        )

        binding.comment.root.setOnClickListener(onCommentClickListener)
        ConfigUtils.getMainFontTypeface()?.let {
            binding.authorName.typeface = it
        }
        binding.authorName.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(binding.root.context))
        binding.comment.previewLayout.visibility = View.VISIBLE
        ConfigUtils.getMainFontTypeface()?.let {
            binding.comment.previewTime.typeface = it
        }
        binding.comment.previewProgress.setOnClickListener {
            onEvent(TicketView.Event.OnCancelUploadClick(id, attachId))
        }


        binding.comment.previewFull.outlineColor =
            ActivityCompat.getColor(binding.root.context, R.color.psd_comment_preview_outline)
        binding.comment.previewFull.outlineRadius =
            binding.root.resources.getDimensionPixelSize(R.dimen.psd_comment_radius)
        binding.comment.previewFull.outlineWidth =
            binding.root.resources.getDimensionPixelSize(R.dimen.psd_comment_preview_outline_radius)

        previewDownloadDrawable = binding.comment.previewProgress.progressDrawable as LayerDrawable
    }


    override fun bind(builder: PayloadActionBuilder<CommentEntry.Comment.CommentPreviewableAttachment>) =
        builder.diff {
            id = entry.id
            hasError = entry.hasError
            attachId = entry.attach.attachId

            this.entry.attach::attachUrl.payloadCheck {
                setPreview(entry.attach.attachUrl)
            }

            this.entry.attach::fileProgressStatus.payloadCheck {
                fileProgressStatus = entry.attach.fileProgressStatus
            }
            this.entry.attach::uploadProgress.payloadCheck {
                setProgress(entry.attach.uploadProgress ?: 0)
            }

            this.entry::isInbound.payloadCheck {
                setIsInboundParameters(entry.isInbound)
                setStatus(Status.Completed, entry.isInbound)
            }
            this.entry::status.payloadCheck {
                setStatus(entry.status, entry.isInbound)
            }

            this.entry::timeText.payloadCheck {
                binding.comment.previewTime.text =
                    entry.timeText?.text(binding.root.context)
            }
            this.entry::showAuthorName.payloadCheck {
                if (!entry.isInbound)
                    binding.authorName.isVisible = entry.showAuthorName
                else
                    binding.authorName.visibility = GONE
            }
            this.entry::authorName.payloadCheck {
                if (!entry.isInbound)
                    binding.authorName.text =
                        entry.authorName?.text(binding.authorName.context)
            }
            this.entry::showAvatar.payloadCheck {
                if (!entry.isInbound)
                    binding.avatar.visibility =
                        if (entry.showAvatar) View.VISIBLE else View.INVISIBLE
                else
                    binding.avatar.visibility = GONE
            }
            this.entry::avatarUrl.payloadCheck {
                if (!entry.isInbound) {
                    val placeHolder =
                        if (entry.isSupport) ConfigUtils.getSupportAvatar(itemView.context)
                        else ConfigUtils.getAuthorAvatar(itemView.context)
                    if (entry.showAvatar) {
                        PyrusServiceDesk.injector().picasso
                            .load(entry.avatarUrl)
                            .placeholder(placeHolder)
                            .transform(CIRCLE_TRANSFORMATION)
                            .into(binding.avatar)
                    }
                }
            }
        }

    override fun clear() {
        super.clear()
        recentPicassoTarget?.cancel()
    }

    private fun setIsInboundParameters(isInbound: Boolean) {
        val backgroundColor = if (!isInbound) {
            ConfigUtils.getSupportMessageTextBackgroundColor(binding.root.context)
        }
        else {
            ConfigUtils.getUserMessageTextBackgroundColor(binding.root.context)
        }
        primaryColor = getTextColorOnBackground(binding.root.context, backgroundColor)
        val secondaryColor = adjustColorChannel(primaryColor, ColorChannel.Alpha, SECONDARY_TEXT_COLOR_MULTIPLIER)
        binding.comment.backgroundParent.setCardBackgroundColor(backgroundColor)

        binding.comment.backgroundParent.setCardBackgroundColor(backgroundColor)

        previewDownloadDrawable.adjustSettingsForProgress(primaryColor, secondaryColor)
        binding.comment.root.gravity = Gravity.BOTTOM or if (!isInbound) Gravity.START else Gravity.END
        binding.authorAndComment.gravity = if (!isInbound) Gravity.START else Gravity.END

        binding.guidelineStart.setGuidelinePercent(if (isInbound) 0.1f else 0.0f)
        binding.guidelineEnd.setGuidelinePercent(if (isInbound) 1f else 0.8f)
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

    /**
     * Assigns [progress] that should shown on progress icon.
     * Works with [ContentType.Attachment].
     */
    private fun setProgress(progress: Int) {
        binding.comment.previewProgress.let {
            ObjectAnimator.ofInt(it, "progress", progress)
                .apply {
                    duration = when (progress) {
                        0 -> 0
                        else -> PROGRESS_CHANGE_ANIMATION_DURATION
                    }
                    interpolator = DecelerateInterpolator()
                }
                .start()
        }
    }

    private fun setPreview(previewUri: Uri) {
        val isRemote = previewUri.isRemote()
        binding.comment.previewProgress.visibility = if (isRemote) GONE else View.VISIBLE
        binding.comment.previewFull.setImageBitmap(null)
        when {
            isRemote -> setNetworkPreview(previewUri, binding.comment.previewFull, true)
            else -> setLocalPreview(binding.comment.previewFull, previewUri, true)
        }
    }

    private fun setNetworkPreview(
        previewUri: Uri,
        target: ImageView,
        adjustTargetDimensions: Boolean,
    ) {

        setNetworkPreviewDelayed(previewUri, target, adjustTargetDimensions, 0)
    }


    private fun setNetworkPreviewDelayed(
        previewUri: Uri,
        target: ImageView,
        adjustTargetDimensions: Boolean,
        delayMs: Long,
    ) {
        recentPicassoTarget?.cancel()

        val allowApplyResult: (Int) -> Boolean = {
            it == previewCallId
        }

        val picassoTarget = when {
            adjustTargetDimensions -> ChangingSizeTarget(
                target,
                previewUri,
                ++previewCallId,
                binding.root.resources.getDimensionPixelSize(R.dimen.psd_recyclerview_item_height_default),
                lifecycleScope = lifecycleOwner.lifecycleScope,
                retryDelay = delayMs,
                ratioMap = previewRatioMap,
                canApplyResult = allowApplyResult,
            )

            else -> SimpleTarget(
                target,
                previewUri,
                ++previewCallId,
                lifecycleScope = lifecycleOwner.lifecycleScope,
                retryDelay = delayMs,
                canApplyResult = allowApplyResult,
            ) { target.visibility = View.VISIBLE }
        }

        recentPicassoTarget = picassoTarget
        PyrusServiceDesk.injector().picasso.load(previewUri).into(picassoTarget)
    }


    private fun setStatus(status: Status, isInbound: Boolean) {

        val visibility = when (status) {
            Status.Error -> View.VISIBLE
            Status.Processing -> View.VISIBLE
            Status.Completed -> if (isInbound) View.INVISIBLE else GONE
        }

        val iconResId = when (status) {
            Status.Error -> R.drawable.psd_error
            Status.Processing -> R.drawable.psd_sync_clock
            Status.Completed -> null
        }

        binding.comment.statusIcon.visibility = visibility
        iconResId?.let {
            binding.comment.statusIcon.setImageResource(it)
            if (status == Status.Processing)
                (binding.comment.statusIcon.drawable as AnimationDrawable).start()
        }

        if (status == Status.Processing)
            binding.comment.statusIcon.setColorFilter(
                ConfigUtils.getSecondaryColorOnMainBackground(
                    binding.root.context
                ), PorterDuff.Mode.SRC_ATOP
            )
        else
            binding.comment.statusIcon.colorFilter = null
    }

    /**
     * Change this if status of downloading/uploading status was changed.
     * This causes change of the appearance of the progress icon.
     */
    private var fileProgressStatus = Status.Completed
        set(value) {
            val icon = AppCompatResources.getDrawable(
                binding.root.context,
                when (value) {
                    Status.Processing -> R.drawable.psd_close
                    Status.Error -> R.drawable.psd_refresh
                    else -> R.drawable.psd_ic_download
                }
            )
            icon?.let {
                previewDownloadDrawable.let { draw ->
                    draw.setDrawableByLayerId(
                        R.id.progress_icon,
                        it.mutate().apply { setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN) })
                    draw.invalidateSelf()
                }
            }
            if (value != Status.Processing)
                setProgress(0)

            applyProgressStatusToPreview(value)

            field = value
        }

    private fun applyProgressStatusToPreview(status: Status) {
        binding.comment.previewProgress.visibility = when (status) {
            Status.Completed -> GONE
            else -> View.VISIBLE
        }
    }

    /**
     * There is a problem with [Picasso] that doesn't allow to use [ChangingSizeTarget] with local files.
     */
    private fun setLocalPreview(
        target: ImageView,
        previewUri: Uri,
        adjustTargetDimension: Boolean,
        onFailed: (() -> Unit)? = null,
    ) {

        val bitmap = try {
            val exif = binding.root.context.contentResolver.openInputStream(previewUri)?.use {
                ExifInterface(it)
            }
            val source = exif?.thumbnailBitmap
                ?: BitmapFactory.decodeStream(
                    binding.root.context.contentResolver.openInputStream(
                        previewUri
                    )
                )

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
            val ratio = bitmap.width.toFloat() / bitmap.height
            if (!previewRatioMap.containsKey(previewUri))
                previewRatioMap += previewUri to ratio
            target.layoutParams.width = getFullSizePreviewWidth(
                previewUri,
                target.layoutParams.height,
                previewRatioMap
            )
            target.requestLayout()
        }
        target.setImageBitmap(bitmap)
    }

    companion object {

        private const val PROGRESS_BACKGROUND_MULTIPLIER = 0.3f
        private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
        private const val PROGRESS_CHANGE_ANIMATION_DURATION = 100L

        private val previewRatioMap = mutableMapOf<Uri, Float>()
    }

}