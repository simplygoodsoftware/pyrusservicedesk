package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.animation.ObjectAnimator
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
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.BYTES_IN_KILOBYTE
import com.pyrus.pyrusservicedesk._ref.utils.BYTES_IN_MEGABYTE
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.adjustColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.getTextColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.isRemote
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentAttachmentBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import com.pyrus.pyrusservicedesk.presentation.ui.view.OutlineImageView.Companion.EDGE_RIGHT
import kotlin.reflect.KClass

internal class CommentAttachmentFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
    private val lifecycleOwner: LifecycleOwner,
) : ItemFingerprint<CommentEntry.Comment.CommentAttachment>() {
    override val layoutId: Int = R.layout.psd_view_holder_comment_attachment

    override val entryKeyKClass: KClass<*> = CommentEntry.Comment.CommentAttachment::class


    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.Comment.CommentAttachment> = CommentAttachmentHolder(
        PsdViewHolderCommentAttachmentBinding.inflate(layoutInflater, parent, false),
        lifecycleOwner,
        onEvent
    )

    override fun areItemsTheSame(
        oldItem: CommentEntry.Comment.CommentAttachment,
        newItem: CommentEntry.Comment.CommentAttachment
    ) = newItem.entryId == oldItem.entryId
}

internal class CommentAttachmentHolder(
    val binding: PsdViewHolderCommentAttachmentBinding,
    private val lifecycleOwner: LifecycleOwner,
    onEvent: (event: TicketView.Event) -> Unit,
) : BaseViewHolder<CommentEntry.Comment.CommentAttachment>(binding.root) {

    var hasError: Boolean = false
    var id: Long = -1
    var attachId: Long = -1

    private var fileDownloadDrawable: LayerDrawable

    @ColorInt
    private var primaryColor: Int = 0

    private var recentFileSize: Float = 0f

    private var previewCallId = 0
    private var recentPicassoTarget: SimpleTarget? = null

    private val onCommentClickListener = View.OnClickListener {
        when {
            hasError -> onEvent(TicketView.Event.OnErrorCommentClick(id))
            fileProgressStatus == Status.Completed ->
                onEvent(TicketView.Event.OnPreviewClick(id, attachId))
        }
    }

    init {

        binding.comment.root.setOnClickListener(onCommentClickListener)
        ConfigUtils.getMainFontTypeface()?.let {
            binding.authorName.typeface = it
        }
        binding.authorName.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(binding.root.context))
        binding.comment.attachmentLayout.visibility = View.VISIBLE
        ConfigUtils.getMainFontTypeface()?.let {
            binding.comment.fileName.typeface = it
            binding.comment.fileSize.typeface = it
            binding.comment.previewMiniTime.typeface = it
        }
        fileDownloadDrawable = binding.comment.attachmentProgress.progressDrawable as LayerDrawable
        binding.comment.previewMini.outlineRadius = binding.root.resources.getDimensionPixelSize(R.dimen.psd_comment_radius)
        binding.comment.previewMini.outlineWidth = binding.root.resources.getDimensionPixelSize(R.dimen.psd_comment_preview_outline_radius)
        binding.comment.previewMini.edges = binding.comment.previewMini.edges and EDGE_RIGHT.inv()
    }


    override fun bind(builder: PayloadActionBuilder<CommentEntry.Comment.CommentAttachment>) =
        builder.diff {
            id = entry.id
            hasError = entry.hasError
            attachId = entry.attach.attachId

            this.entry.attach::attachUrl.payloadCheck {
                setMiniPreview(entry.attach.attachUrl)
            }
            this.entry.attach::attachmentName.payloadCheck {
                binding.comment.fileName.text = entry.attach.attachmentName
            }

            this.entry.attach::fileSize.payloadCheck {
                setFileSize(entry.attach.fileSize)
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
                binding.comment.previewMiniTime.text =
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
        binding.comment.fileName.setTextColor(primaryColor)
        binding.comment.fileSize.setTextColor(secondaryColor)
        binding.comment.previewMiniTime.setTextColor(secondaryColor)

        binding.comment.previewMini.outlineColor = backgroundColor
        fileDownloadDrawable.adjustSettingsForProgress(primaryColor, secondaryColor)

        binding.comment.root.gravity =
            Gravity.BOTTOM or if (!isInbound) Gravity.START else Gravity.END
        binding.authorAndComment.gravity = if (!isInbound) Gravity.START else Gravity.END

        binding.guidelineStart.setGuidelinePercent(if (isInbound) 0.1f else 0.0f)
        binding.guidelineEnd.setGuidelinePercent(if (isInbound) 1f else 0.8f)
    }

    private fun setMiniPreview(previewUri: Uri) {
        binding.comment.previewMini.setImageBitmap(null)
        binding.comment.previewMini.visibility = GONE
        when {
            previewUri.isRemote() -> setNetworkPreview(
                previewUri,
                binding.comment.previewMini,
                false
            )
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
                previewRatioMap,
                allowApplyResult,
            )

            else -> SimpleTarget(
                target,
                previewUri,
                ++previewCallId,
                lifecycleScope = lifecycleOwner.lifecycleScope,
                retryDelay = delayMs,
                allowApplyResult,
            ) { target.visibility = View.VISIBLE }
        }
        recentPicassoTarget = picassoTarget
        PyrusServiceDesk.injector().picasso.load(previewUri).into(picassoTarget)
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
     * Uses [bytesSize] to render the size of the file.
     * Works with [ContentType.Attachment].
     */
    private fun setFileSize(bytesSize: Float) {
        recentFileSize = bytesSize
        val isMegabytes = recentFileSize >= BYTES_IN_MEGABYTE / 10
        val toShow = when {
            isMegabytes -> recentFileSize / BYTES_IN_MEGABYTE
            else -> recentFileSize / BYTES_IN_KILOBYTE
        }
        val textResId = when {
            isMegabytes -> R.string.psd_file_size_mb
            else -> R.string.psd_file_size_kb
        }
        binding.comment.fileSize.text = binding.root.resources.getString(textResId, toShow)
    }

    /**
     * Assigns [progress] that should shown on progress icon.
     * Works with [ContentType.Attachment].
     */
    private fun setProgress(progress: Int) {
        binding.comment.attachmentProgress.let {
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

    private fun applyProgressStatusToAttachmentView(status: Status) {
        if (status != Status.Processing)
            setFileSize(recentFileSize)
        else
            binding.comment.fileSize.setText(R.string.psd_uploading)
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
                fileDownloadDrawable.let { draw ->
                    draw.setDrawableByLayerId(
                        R.id.progress_icon,
                        it.mutate().apply { setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN) })
                    draw.invalidateSelf()
                }
            }
            if (value != Status.Processing)
                setProgress(0)

            applyProgressStatusToAttachmentView(value)

            field = value
        }

    companion object {

        private const val PROGRESS_BACKGROUND_MULTIPLIER = 0.3f
        private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
        private const val PROGRESS_CHANGE_ANIMATION_DURATION = 100L

        private val previewRatioMap = mutableMapOf<Uri, Float>()
    }

}