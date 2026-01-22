package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.adjustColorChannel
import com.pyrus.pyrusservicedesk._ref.utils.getTextColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentTextBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import com.pyrus.pyrusservicedesk.presentation.ui.view.LinkUtils
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.KClass

internal class CommentTextFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ItemFingerprint<CommentEntry.Comment.CommentText>() {
    override val layoutId: Int = R.layout.psd_view_holder_comment_text

    override val entryKeyKClass: KClass<*> = CommentEntry.Comment.CommentText::class


    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
    ): BaseViewHolder<CommentEntry.Comment.CommentText> = CommentTextHolder(
        PsdViewHolderCommentTextBinding.inflate(layoutInflater, parent, false),
        onEvent
    )

    override fun areItemsTheSame(
        oldItem: CommentEntry.Comment.CommentText,
        newItem: CommentEntry.Comment.CommentText,
    ) = newItem.entryId == oldItem.entryId

    override fun getChangePayload(
        oldItem: CommentEntry.Comment.CommentText,
        newItem: CommentEntry.Comment.CommentText,
    ): Any? {
        val payload = HashSet<String>()

        if (oldItem.creationTime != newItem.creationTime) payload.add(getPropertyName(CommentEntry.Comment.CommentText::creationTime))
        if (oldItem.entryId != newItem.entryId) payload.add(getPropertyName(CommentEntry.Comment.CommentText::entryId))
        if (oldItem.id != newItem.id) payload.add(getPropertyName(CommentEntry.Comment.CommentText::id))
        if (oldItem.isInbound != newItem.isInbound) payload.add(getPropertyName(CommentEntry.Comment.CommentText::isInbound))
        if (oldItem.isSupport != newItem.isSupport) payload.add(getPropertyName(CommentEntry.Comment.CommentText::isSupport))
        if (oldItem.hasError != newItem.hasError) payload.add(getPropertyName(CommentEntry.Comment.CommentText::hasError))
        if (oldItem.isLocal != newItem.isLocal) payload.add(getPropertyName(CommentEntry.Comment.CommentText::isLocal))
        if (oldItem.isWelcomeMessage != newItem.isWelcomeMessage) payload.add(getPropertyName(CommentEntry.Comment.CommentText::isWelcomeMessage))
        if (oldItem.timeText != newItem.timeText) payload.add(getPropertyName(CommentEntry.Comment.CommentText::timeText))
        if (oldItem.status != newItem.status) payload.add(getPropertyName(CommentEntry.Comment.CommentText::status))
        if (oldItem.authorName != newItem.authorName) payload.add(getPropertyName(CommentEntry.Comment.CommentText::authorName))
        if (oldItem.authorKey != newItem.authorKey) payload.add(getPropertyName(CommentEntry.Comment.CommentText::authorKey))
        if (oldItem.showAuthorName != newItem.showAuthorName) payload.add(getPropertyName(CommentEntry.Comment.CommentText::showAuthorName))
        if (oldItem.avatarUrl != newItem.avatarUrl) payload.add(getPropertyName(CommentEntry.Comment.CommentText::avatarUrl))
        if (oldItem.showAvatar != newItem.showAvatar) payload.add(getPropertyName(CommentEntry.Comment.CommentText::showAvatar))
        if (oldItem.text != newItem.text) payload.add(getPropertyName(CommentEntry.Comment.CommentText::text))

        return payload
    }
}

internal class CommentTextHolder(
    val binding: PsdViewHolderCommentTextBinding,
    onEvent: (event: TicketView.Event) -> Unit,
) : BaseViewHolder<CommentEntry.Comment.CommentText>(binding.root) {

    var content: String? = null
    var hasError: Boolean = false
    var id: Long = -1

    private val onCommentLongClickListener = View.OnLongClickListener {
        val cont = content
        return@OnLongClickListener if (!cont.isNullOrBlank()) {
            onEvent(TicketView.Event.OnCopyClick(cont))
            true
        }
        else {
            false
        }
    }

    private val onCommentClickListener = View.OnClickListener {
        when {
            hasError -> onEvent(TicketView.Event.OnErrorCommentClick(id))
        }
    }

    init {
        itemView.setOnLongClickListener(onCommentLongClickListener)
        itemView.setOnClickListener(onCommentClickListener)
        binding.comment.root.setOnLongClickListener(onCommentLongClickListener)
        binding.comment.root.setOnClickListener(onCommentClickListener)
        binding.comment.commentText.setOnLongClickListener(onCommentLongClickListener)
        ConfigUtils.getMainFontTypeface()?.let {
            binding.authorName.typeface = it
        }
        binding.authorName.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(binding.root.context))
        binding.comment.commentTextLayout.visibility = View.VISIBLE
        ConfigUtils.getMainFontTypeface()?.let {
            binding.comment.commentText.typeface = it
            binding.comment.textTime.typeface = it
        }
    }


    override fun bind(builder: PayloadActionBuilder<CommentEntry.Comment.CommentText>) = builder.diff {
            content = entry.text
            id = entry.id
            hasError = entry.hasError
            setStatus(entry.status, entry.isInbound)


            setIsInboundParameters(entry.isInbound)

            val filteredText = entry.text
            binding.comment.commentText.text = replaceLinkTagsWithSpans(filteredText)
            LinkifyCompat.addLinks(
                binding.comment.commentText,
                Linkify.WEB_URLS or Linkify.PHONE_NUMBERS
            )
            addDeepLinks(binding.comment.commentText)
            binding.comment.commentText.movementMethod = LinkMovementMethod.getInstance()


            binding.comment.textTime.text =
                entry.timeText?.text(binding.root.context)

            if (!entry.isInbound)
                binding.authorName.isVisible = entry.showAuthorName
            else
                binding.authorName.visibility = View.GONE

            if (!entry.isInbound)
                binding.authorName.text =
                    entry.authorName?.text(binding.authorName.context)

            binding.avatar.visibility =
                if (entry.showAvatar) View.VISIBLE else View.INVISIBLE
            if (entry.isInbound) binding.avatar.visibility = View.GONE

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

    private fun setIsInboundParameters(isInbound: Boolean) {
        val backgroundColor = if (!isInbound) {
            ConfigUtils.getSupportMessageTextBackgroundColor(binding.root.context)
        }
        else {
            ConfigUtils.getUserMessageTextBackgroundColor(binding.root.context)
        }
        val primaryColor = getTextColorOnBackground(binding.root.context, backgroundColor)
        val secondaryColor = adjustColorChannel(primaryColor, ColorChannel.Alpha, SECONDARY_TEXT_COLOR_MULTIPLIER)

        binding.comment.backgroundParent.setCardBackgroundColor(backgroundColor)

        val textColor = if (!isInbound)
            ConfigUtils.getSupportMessageTextColor(binding.root.context, backgroundColor)
        else
            ConfigUtils.getUserMessageTextColor(binding.root.context, backgroundColor)

        binding.comment.commentText.setTextColor(textColor)

        val linkColor = when {
            isInbound -> primaryColor
            else -> ConfigUtils.getAccentColor(binding.root.context)
        }
        binding.comment.commentText.setLinkTextColor(linkColor)
        binding.comment.textTime.setTextColor(secondaryColor)


        binding.comment.root.gravity = Gravity.BOTTOM or if (!isInbound) Gravity.START else Gravity.END
        binding.authorAndComment.gravity = if (!isInbound) Gravity.START else Gravity.END

        binding.guidelineStart.setGuidelinePercent(if (isInbound) 0.1f else 0.0f)
        binding.guidelineEnd.setGuidelinePercent(if (isInbound) 1f else 0.8f)
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
                LinkUtils.createClickableSpan(span.first, binding.root.context, span.second),
                span.third.first,
                span.third.last,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        return ssb
    }

    private fun addDeepLinks(textView: AppCompatTextView) {
        val ssb = SpannableStringBuilder(textView.text)
        val matcher: Matcher = Pattern.compile("(\\S+)://\\S+").matcher(ssb)

        var anyFound = false
        while (matcher.find()) {
            val group = matcher.group(1)?.lowercase()
            if (group == "http" || group == "https") {
                continue
            }

            anyFound = true

            val clickableSpan = LinkUtils.createClickableSpan(matcher.group(), binding.root.context)

            ssb.setSpan(clickableSpan, matcher.start(), matcher.end(), 0)
        }
        if (anyFound) {
            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.text = ssb
        }

    }

    companion object {
        private const val SECONDARY_TEXT_COLOR_MULTIPLIER = 0.5f
    }

}