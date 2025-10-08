package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout.GONE
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.data.AudioData
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.adjustColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.getTextColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getTimeString
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentAudioBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.KClass


internal class CommentAudioFingerprint(
    private val audioPlayer: AudioWrapper,
    private val coroutineScope: CoroutineScope,
    private val onEvent: (TicketView.Event) -> Unit
) : ItemFingerprint<CommentEntry.Comment.CommentAudio>() {
    override val layoutId: Int = R.layout.psd_view_holder_comment_audio

    override val entryKeyKClass: KClass<*> = CommentEntry.Comment.CommentAudio::class


    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.Comment.CommentAudio> = CommentAudioHolder(
        PsdViewHolderCommentAudioBinding.inflate(layoutInflater, parent, false),
        audioPlayer,
        coroutineScope,
        onEvent,
    )

    override fun areItemsTheSame(
        oldItem: CommentEntry.Comment.CommentAudio,
        newItem: CommentEntry.Comment.CommentAudio
    ) = newItem.entryId == oldItem.entryId
}

internal class CommentAudioHolder(
    private val binding: PsdViewHolderCommentAudioBinding,
    private val audioPlayer: AudioWrapper,
    private val coroutineScope: CoroutineScope,
    private val onEvent: (TicketView.Event) -> Unit
) : BaseViewHolder<CommentEntry.Comment.CommentAudio>(binding.root) {

    private var hasError: Boolean = false

    private var id: Long = -1
    private var attachId: Long = -1

    private var attachName: String = ""
    private var isLocal: Boolean = false
    private var attachUri: String? = null
    private var fullUrl: String? = null

    @ColorInt
    private var primaryColor: Int = 0

    private var listenDataJob: Job? = null

    private var userIsSeeking = false


    init {

        binding.comment.playButton.setOnClickListener {
            if (isLocal) {
                onEvent(TicketView.Event.OnCancelUploadClick(id, attachId))
            }
            else {
                attachUri?.let{ fullUrl?.let { fullUrl -> audioPlayer.playAudio(it, fullUrl) } }
            }
        }
        binding.root.setOnClickListener {
            when {
                hasError -> onEvent(TicketView.Event.OnErrorCommentClick(id))
            }
        }
        ConfigUtils.getMainFontTypeface()?.let {
            binding.authorName.typeface = it
        }
        binding.authorName.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(binding.root.context))

        binding.comment.playerProgressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                userIsSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                userIsSeeking = false
                val url = attachUri
                if (url != null) {
                    audioPlayer.waitForSeek(binding.comment.playerProgressBar.progress.toLong(), url)
                }
            }

        })

    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.Comment.CommentAudio>) = builder.diff {
        hasError = entry.hasError
        attachId = entry.attach.attachId
        id = entry.id
        attachName = entry.attach.attachmentName
        isLocal = entry.isLocal

        entry.attach::attachUrl.payloadCheck {
            val attachUrl = entry.attach.attachUrl.toString()
            val start = if (attachUrl.contains("DownloadFile/"))
                attachUrl.indexOf("DownloadFile/").plus("DownloadFile/".length)
                else 0
            val end = if (attachUrl.contains("?user_id=")) attachUrl.indexOf("?user_id=")
                else attachUrl.length
            attachUri = attachUrl.substring(start, end)
            fullUrl = attachUrl
            listenDataJob?.cancel()
            listenDataJob = this@CommentAudioHolder.coroutineScope.launch {
                this@CommentAudioHolder.audioPlayer.getAudioDataFlow(attachUrl.substring(start, end)).collect(::applyAudioData)
            }
        }

        entry::isInbound.payloadCheck {
            setIsInboundParameters(entry.isInbound)
        }

        entry::timeText.payloadCheck {
            binding.comment.commentTime.text = entry.timeText?.text(binding.root.context)
        }
        entry::showAuthorName.payloadCheck {
            if (!entry.isInbound)
                binding.authorName.isVisible = entry.showAuthorName
            else
                binding.authorName.visibility = GONE
        }
        entry::authorName.payloadCheck {
            if (!entry.isInbound)
                binding.authorName.text =
                    entry.authorName?.text(binding.authorName.context)
        }
        entry::showAvatar.payloadCheck {
            if (!entry.isInbound)
                binding.avatar.visibility =
                    if (entry.showAvatar) View.VISIBLE
                    else View.INVISIBLE
            else
                binding.avatar.visibility = View.INVISIBLE
        }
        entry::avatarUrl.payloadCheck {
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
        entry::isInbound.payloadCheck {
            setIsInboundParameters(entry.isInbound)
            setStatus(Status.Completed, entry.isInbound)
        }
        entry::status.payloadCheck {
            setStatus(entry.status, entry.isInbound)
        }
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
        binding.comment.trackCurrentTime.setTextColor(secondaryColor)
        binding.comment.trackFullTime.setTextColor(secondaryColor)
        binding.comment.trackTimeDivider.setTextColor(secondaryColor)
        binding.comment.commentTime.setTextColor(secondaryColor)
        val playButtonColor = if (!isInbound) {
            ConfigUtils.getAccentColor(itemView.context)
        }
        else {
            itemView.resources.getColor(R.color.psd_color_blue_white_01)
        }

        val progressTintColor = if (!isInbound) {
            ConfigUtils.getAccentColor(itemView.context)
        }
        else {
            itemView.resources.getColor(R.color.psd_white)
        }

        val progressBackgroundTintColor = if (!isInbound) {
            itemView.resources.getColor(R.color.psd_color_black_01)
        }
        else {
            itemView.resources.getColor(R.color.psd_color_white_03)
        }

        binding.comment.playButton.backgroundTintList = ColorStateList.valueOf(playButtonColor)
        binding.comment.playerProgressBar.progressTintList = ColorStateList.valueOf(progressTintColor)
        binding.comment.playerProgressBar.thumbTintList = ColorStateList.valueOf(progressTintColor)
        binding.comment.playerProgressBar.progressBackgroundTintList = ColorStateList.valueOf(progressBackgroundTintColor)
        binding.comment.root.gravity =
            Gravity.BOTTOM or if (!isInbound) Gravity.START else Gravity.END
        binding.authorAndComment.gravity = if (!isInbound) Gravity.START else Gravity.END

        binding.guidelineStart.setGuidelinePercent(if (isInbound) 0.1f else 0.0f)
        binding.guidelineEnd.setGuidelinePercent(if (isInbound) 1f else 0.9f)
    }

    private fun applyAudioData(data: AudioData) {
        binding.comment.playerProgressBar.max = data.audioFullTime?.toInt() ?: 0
        updateSeekBar(data.position)
        updateAudioStatus(data.status)
        binding.comment.trackCurrentTime.text = getTimeString(binding.root.context, data.audioCurrentTime?.toDouble() ?: 0.0)
    }

    private fun updateSeekBar(position: Int) {
        if (userIsSeeking) return
        binding.comment.playerProgressBar.progress = position
    }

    private fun updateAudioStatus(status: AudioStatus) {
        if (isLocal) {
            binding.comment.downloadProgressBar.isVisible = true
            binding.comment.playButton.setImageResource(R.drawable.ic_audio_downliading_cansel)
            return
        }
        when (status) {
            AudioStatus.Paused -> {
                binding.comment.downloadProgressBar.isVisible = false
                binding.comment.playButton.setImageResource(R.drawable.psd_ic_audio_play)
            }
            AudioStatus.None -> {
                binding.comment.downloadProgressBar.isVisible = false
                binding.comment.playButton.setImageResource(R.drawable.psd_ic_audio_download)
            }
            AudioStatus.Error -> {
                binding.comment.downloadProgressBar.isVisible = false
                binding.comment.playButton.setImageResource(R.drawable.psd_ic_audio_download)
            }
            AudioStatus.Playing -> {
                binding.comment.downloadProgressBar.isVisible = false
                binding.comment.playButton.setImageResource(R.drawable.psd_ic_audio_pause)
            }
            AudioStatus.Processing -> {
                binding.comment.downloadProgressBar.isVisible = true
                binding.comment.playButton.setImageResource(R.drawable.ic_audio_downliading_cansel)
            }
        }
    }

    companion object {
        private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
    }

}