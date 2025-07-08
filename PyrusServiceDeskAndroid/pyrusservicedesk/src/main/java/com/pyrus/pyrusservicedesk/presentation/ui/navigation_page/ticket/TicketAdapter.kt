package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import com.google.android.flexbox.FlexboxLayout
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.ButtonEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.ButtonsEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.DateEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.RatingEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.SimpleRatingEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.TicketEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.Type
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.WelcomeMessageEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceMultiplier
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getAvatarUrl
import com.pyrus.pyrusservicedesk.utils.getTimeText
import com.pyrus.pyrusservicedesk.utils.isImage


private const val VIEW_TYPE_COMMENT_INBOUND = 0
private const val VIEW_TYPE_COMMENT_OUTBOUND = 1
private const val VIEW_TYPE_WELCOME_MESSAGE = 2
private const val VIEW_TYPE_DATE = 3
private const val VIEW_TYPE_RATING = 4
private const val VIEW_TYPE_COMMENT_RATING = 5
private const val VIEW_TYPE_COMMENT_BUTTONS = 6

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketAdapter: AdapterBase<TicketEntry>() {

    companion object {
        const val MAX_SYMBOLS_BEFORE_LEFT_ALIGNMENT = 8
        const val PYRUS_SYSTEM_AUTHOR_NAME = "Pyrus System"
    }

    /**
     * [SpaceMultiplier] implementation for customizing spaces between items fot the feed.
     */
    val itemSpaceMultiplier = object: SpaceMultiplier{
        override fun getMultiplier(adapterPosition: Int): Float {
            return when {
                adapterPosition <= 0 -> 1f
                itemsList[adapterPosition].type == Type.Comment
                        && itemsList[adapterPosition -1].type == Type.Comment
                        && (itemsList[adapterPosition] as CommentEntry).comment.isInbound !=
                            (itemsList[adapterPosition - 1] as CommentEntry).comment.isInbound -> 2f
                else -> 1f
            }
        }
    }
    private var onFileReadyToPreviewClickListener: ((Attachment) -> Unit)? = null
    private var onTextCommentLongClicked: ((String) -> Unit)? = null
    private var onErrorCommentEntryClickListener: ((CommentEntry) -> Unit)? = null
    private var recentInboundCommentPositionWithAvatar = 0
    private var onRatingClickListener: ((Int) -> Unit)? = null

    override fun getItemViewType(position: Int): Int {
        return with(itemsList[position]) {
            return@with when {
                type == Type.Date -> VIEW_TYPE_DATE
                type == Type.WelcomeMessage -> VIEW_TYPE_WELCOME_MESSAGE
                type == Type.Rating -> VIEW_TYPE_RATING
                type == Type.Buttons -> VIEW_TYPE_COMMENT_BUTTONS
                (this as CommentEntry).comment.rating != null -> VIEW_TYPE_COMMENT_RATING
                this.comment.isInbound -> VIEW_TYPE_COMMENT_OUTBOUND
                else -> VIEW_TYPE_COMMENT_INBOUND
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<TicketEntry> {
        return when(viewType){
            VIEW_TYPE_COMMENT_INBOUND -> InboundCommentHolder(parent)
            VIEW_TYPE_COMMENT_OUTBOUND -> OutboundCommentHolder(parent)
            VIEW_TYPE_WELCOME_MESSAGE -> WelcomeMessageHolder(parent)
            VIEW_TYPE_RATING -> RatingHolder(parent)
            VIEW_TYPE_COMMENT_RATING -> RatingCommentHolder(parent)
            VIEW_TYPE_COMMENT_BUTTONS -> ButtonsHolder(parent)
            else -> DateViewHolder(parent)
        } as ViewHolderBase<TicketEntry>
    }

    override fun onViewAttachedToWindow(holder: ViewHolderBase<TicketEntry>) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.translationX = 0f
        holder.itemView.findViewById<View>(R.id.author_and_comment)?.let { it.translationX = 0f }
    }

    override fun setItems(items: List<TicketEntry>) {
        this.itemsList = items.toMutableList()
    }

    /**
     * Assigns [listener] ths is invoked when comment with the file that is
     * ready to be previewed was clicked.
     */
    fun setOnFileReadyForPreviewClickListener(listener: (attachment: Attachment) -> Unit) {
        onFileReadyToPreviewClickListener = listener
    }

    /**
     * Assigns [listener] that is invoked when comment with error was clicked.
     */
    fun setOnErrorCommentEntryClickListener(listener: (entry: CommentEntry) -> Unit) {
        onErrorCommentEntryClickListener = listener
    }

    /**
     * Assigns [listener] that is invoked when text comment was long pressed by the user.
     */
    fun setOnTextCommentLongClicked(listener: (String) -> Unit) {
        onTextCommentLongClicked = listener
    }

    fun setOnRatingClickListener(listener: ((Int) -> Unit)) {
        onRatingClickListener = listener
    }

    private inner class InboundCommentHolder(parent: ViewGroup) :
        CommentHolder(parent, R.layout.psd_view_holder_comment_inbound) {

        override val comment: CommentView = itemView.findViewById(R.id.comment)
        private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        private val authorName = itemView.findViewById<TextView>(R.id.author_name)

        init {
            ConfigUtils.getMainFontTypeface()?.let {
                authorName.typeface = it
            }
            authorName.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(parent.context))
        }

        override fun bindItem(item: CommentEntry) {
            super.bindItem(item)
            setAuthorNameAndVisibility(shouldShowAuthorName())
            with(shouldShowAuthorAvatar()) {
                setAuthorAvatarVisibility(this)
                if (this && shouldRedrawRecentCommentWithAvatar()) {
                    val toRedraw = recentInboundCommentPositionWithAvatar
                    itemView.post { notifyItemChanged(toRedraw) }
                    recentInboundCommentPositionWithAvatar = absoluteAdapterPosition
                }
            }
        }

        private fun shouldRedrawRecentCommentWithAvatar(): Boolean =
            absoluteAdapterPosition == itemsList.lastIndex && recentInboundCommentPositionWithAvatar != absoluteAdapterPosition

        private fun setAuthorNameAndVisibility(visible: Boolean) {
            authorName.visibility = if (visible) VISIBLE else GONE
            authorName.text = getItem().comment.author.name
        }

        private fun setAuthorAvatarVisibility(visible: Boolean) {
            avatar.visibility = if (visible) VISIBLE else INVISIBLE
            if (visible) {
                PyrusServiceDesk.get().picasso
                    .load(getAvatarUrl(getItem().comment.author.avatarId, PyrusServiceDesk.get().domain))
                    .placeholder(ConfigUtils.getSupportAvatar(itemView.context))
                    .transform(CIRCLE_TRANSFORMATION)
                    .into(avatar)
            }
        }

        private fun shouldShowAuthorName(): Boolean {
            return absoluteAdapterPosition == 0
                    || with(itemsList[absoluteAdapterPosition - 1]) {
                when {
                    this.type != Type.Comment -> true
                    getItem().comment.author.name == PYRUS_SYSTEM_AUTHOR_NAME -> false
                    else -> getItem().comment.author != (this as CommentEntry).comment.author
                }
            }
        }

        private fun shouldShowAuthorAvatar(): Boolean {
            return with(itemsList.getOrNull(absoluteAdapterPosition + 1)) {
                when {
                    this?.type != Type.Comment -> true
                    else -> getItem().comment.author != (this as CommentEntry).comment.author
                }
            }
        }
    }

    private inner class OutboundCommentHolder(parent: ViewGroup)
        : CommentHolder(parent, R.layout.psd_view_holder_comment_outbound) {

        override val comment: CommentView = itemView.findViewById(R.id.comment)

    }

    private abstract inner class CommentHolder(
        parent: ViewGroup,
        @LayoutRes layoutRes: Int,
    )
        : ViewHolderBase<CommentEntry>(parent, layoutRes){

        abstract val comment: CommentView

        val onCommentClickListener = OnClickListener {
            when {
                getItem().hasError() -> onErrorCommentEntryClickListener?.invoke(getItem())
                (comment.contentType == ContentType.Attachment
                        || comment.contentType == ContentType.PreviewableAttachment)
                        && comment.fileProgressStatus == Status.Completed -> {

                    onFileReadyToPreviewClickListener?.invoke(getItem().comment.attachments!!.first())
                }
            }
        }

        val onCommentLongClickListener = OnLongClickListener {
            return@OnLongClickListener when {
                !getItem().comment.hasAttachments() -> {
                    onTextCommentLongClicked?.invoke(getItem().comment.body ?: "")
                    true
                }
                else -> false
            }
        }

        override fun bindItem(item: CommentEntry) {
            super.bindItem(item)
            itemView.setOnClickListener {
                if (getItem().hasError())
                    onErrorCommentEntryClickListener?.invoke(getItem())
            }
            comment.setOnLongClickListener(onCommentLongClickListener)
            comment.setOnClickListener(onCommentClickListener)
            comment.status = when {
                getItem().hasError() -> Status.Error
                getItem().comment.isLocal() -> Status.Processing
                else -> Status.Completed
            }
            comment.contentType = when {
                !item.comment.hasAttachments() -> ContentType.Text
                item.comment.attachments!!.first().name.isImage() -> ContentType.PreviewableAttachment
                else -> ContentType.Attachment
            }
            when (comment.contentType){
                ContentType.Text -> bindTextView()
                else -> bindAttachmentView()
            }

            val creationTime =
                if (getItem().comment.isWelcomeMessage) ""
                else getItem().comment.creationDate.getTimeText(itemView.context)

            comment.setCreationTime(creationTime)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            getItem().uploadFileHooks?.unsubscribeFromProgress()
        }

        private fun bindTextView() {
            val text = getItem().comment.body ?: ""
            comment.setCommentText(text)
        }

        private fun bindAttachmentView() {
            getItem().comment.attachments!!.first().let { it ->
                comment.setFileName(getItem().comment.attachments?.first()?.name ?: "")
                comment.setFileSize(getItem().comment.attachments?.first()?.bytesSize?.toFloat() ?: 0f)
                comment.setPreview(it.getPreviewUrl())
                comment.fileProgressStatus = if (getItem().hasError()) Status.Error else Status.Completed
                comment.setOnProgressIconClickListener {
                    when (comment.fileProgressStatus) {
                        Status.Processing -> getItem().uploadFileHooks?.cancelUploading()
                        Status.Completed -> onFileReadyToPreviewClickListener?.invoke(getItem().comment.attachments!![0])
                        Status.Error -> onCommentClickListener.onClick(comment)
                    }
                }
                if (!getItem().hasError()) {
                    getItem().uploadFileHooks?.subscribeOnProgress {
                        comment.setProgress(it)
                        when {
                            it == itemView.resources.getInteger(R.integer.psd_progress_max_value) ->
                                comment.fileProgressStatus = Status.Completed
                            comment.fileProgressStatus != Status.Processing ->
                                comment.fileProgressStatus = Status.Processing
                        }
                    }
                }
            }
        }
    }

    private class WelcomeMessageHolder(parent: ViewGroup) :
        ViewHolderBase<WelcomeMessageEntry>(parent, R.layout.psd_view_holder_comment_inbound) {

        private val comment: CommentView = itemView.findViewById(R.id.comment)
        private val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        private val authorName = itemView.findViewById<TextView>(R.id.author_name)

        init {
            ConfigUtils.getMainFontTypeface()?.let {
                authorName.typeface = it
            }
        }

        override fun bindItem(item: WelcomeMessageEntry) {
            super.bindItem(item)
            authorName.visibility = GONE
            avatar.visibility = INVISIBLE
            comment.contentType = ContentType.Text
            comment.setCommentText(item.message)
        }
    }

    private class DateViewHolder(parent: ViewGroup)
        : ViewHolderBase<DateEntry>(parent, R.layout.psd_view_holder_date) {

        private val date = itemView.findViewById<TextView>(R.id.date)

        init {
            ConfigUtils.getMainFontTypeface()?.let {
                date.typeface = it
            }
            date.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(itemView.context))
        }

        override fun bindItem(item: DateEntry) {
            super.bindItem(item)
            date.text = item.date
        }
    }

    private inner class RatingHolder(parent: ViewGroup) :
        ViewHolderBase<RatingEntry>(parent, R.layout.psd_view_holder_rating) {


        private var listEntry: List<SimpleRatingEntry> = emptyList()
        private val adapter = RatingAdapter(listEntry)

        //private val ratingRv = itemView.findViewById<RecyclerView>(R.id.ratingRv)

        private val rating1 = itemView.findViewById<View>(R.id.rating1)
        private val rating2 = itemView.findViewById<View>(R.id.rating2)
        private val rating3 = itemView.findViewById<View>(R.id.rating3)
        private val rating4 = itemView.findViewById<View>(R.id.rating4)
        private val rating5 = itemView.findViewById<View>(R.id.rating5)

        private val ratingText1 = itemView.findViewById<TextView>(R.id.ratingText1)
        private val ratingText2 = itemView.findViewById<TextView>(R.id.ratingText2)
        private val ratingText3 = itemView.findViewById<TextView>(R.id.ratingText3)
        private val ratingText4 = itemView.findViewById<TextView>(R.id.ratingText4)
        private val ratingText5 = itemView.findViewById<TextView>(R.id.ratingText5)

        init {
//            ConfigUtils.getSecondaryColorOnMainBackground(itemView.context).apply {
//                rating1.setBackgroundColor(this)
//                rating2.setBackgroundColor(this)
//                rating3.setBackgroundColor(this)
//                rating4.setBackgroundColor(this)
//                rating5.setBackgroundColor(this)
//            }

            rating1.setOnClickListener { onRatingClickListener?.invoke(1)}
            rating2.setOnClickListener { onRatingClickListener?.invoke(2)}
            rating3.setOnClickListener { onRatingClickListener?.invoke(3)}
            rating4.setOnClickListener { onRatingClickListener?.invoke(4)}
            rating5.setOnClickListener { onRatingClickListener?.invoke(5)}

            adapter.setOnRatingClickListener { onRatingClickListener }
        }

        override fun bindItem(item: RatingEntry) {
            super.bindItem(item)
//            ratingRv.apply {
//                adapter = adapter
//                layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
//            }
//
//            listEntry = map(item)
            val listValues = item.ratingSettings?.ratingTextValues
            if (listValues != null) {
                rating1.isVisible = listValues.find { it.rating == 1 } != null
                ratingText1.text = listValues.find { it.rating == 1 }?.text
                rating2.isVisible = listValues.find { it.rating == 2 } != null
                ratingText2.text = listValues.find { it.rating == 2 }?.text
                rating3.isVisible = listValues.find { it.rating == 3 } != null
                ratingText3.text = listValues.find { it.rating == 3 }?.text
                rating4.isVisible = listValues.find { it.rating == 4 } != null
                ratingText4.text = listValues.find { it.rating == 4 }?.text
                rating5.isVisible = listValues.find { it.rating == 5 } != null
                ratingText5.text = listValues.find { it.rating == 5 }?.text
            }
            ratingText1.isVisible = item.ratingSettings?.type == 3
            ratingText2.isVisible = item.ratingSettings?.type == 3
            ratingText3.isVisible = item.ratingSettings?.type == 3
            ratingText4.isVisible = item.ratingSettings?.type == 3
            ratingText5.isVisible = item.ratingSettings?.type == 3
            if (item.ratingSettings?.size != null) {
                rating2.isVisible = item.ratingSettings.size == 5
                rating3.isVisible = item.ratingSettings.size >= 3
                rating4.isVisible = item.ratingSettings.size == 5
            }
            rating5.isVisible = true

        }
    }

    /*private fun map(ratingEntry: RatingEntry): List<SimpleRatingEntry> {
        val listEntry: MutableList<SimpleRatingEntry> = mutableListOf()
        if (ratingEntry.ratingSettings?.ratingTextValues != null) {
            for (rating in ratingEntry.ratingSettings.ratingTextValues) {
                rating.rating?.let { listEntry.add(SimpleRatingEntry(it, rating.rating.ratingToEmojiRes(), rating.text)) }
            }
            return listEntry
        }
        if (ratingEntry.ratingSettings?.type == 1)
            return ratingEntry.ratingSettings.size?.let { getRatingList(listEntry, it) } ?: emptyList()

        return emptyList()
    }

    private fun getRatingList(
        listEntry: MutableList<SimpleRatingEntry>,
        size: Int,
    ): List<SimpleRatingEntry> {

        listEntry.add(SimpleRatingEntry(1, 1.ratingToEmojiRes(), null))
        listEntry.add(SimpleRatingEntry(5, 5.ratingToEmojiRes(), null))
        if (size == 2)
            return listEntry
        listEntry.add(1, SimpleRatingEntry(3, 3.ratingToEmojiRes(), null))
        if (size == 3)
            return listEntry
        listEntry.add(1, SimpleRatingEntry(2, 2.ratingToEmojiRes(), null))
        listEntry.add(3, SimpleRatingEntry(4, 4.ratingToEmojiRes(), null))
        return listEntry

    }*/


    private fun Int?.ratingToEmojiRes(): Int {
        return when (this) {
            1 -> R.drawable.ic_emoji_rating_1
            2 -> R.drawable.ic_emoji_rating_2
            3 -> R.drawable.ic_emoji_rating_3
            4 -> R.drawable.ic_emoji_rating_4
            5 -> R.drawable.ic_emoji_rating_5
            else -> R.drawable.ic_emoji_rating_3
        }
    }

    private inner class RatingCommentHolder(parent: ViewGroup) :
        ViewHolderBase<CommentEntry>(parent, R.layout.psd_view_holder_comment_rating) {

        private val ratingImage = itemView.findViewById<ImageView>(R.id.ratingImage)
        private val statusIcon = itemView.findViewById<ImageView>(R.id.statusIcon)
        override fun bindItem(item: CommentEntry) {
            super.bindItem(item)
            with(itemView) {
                ratingImage.setImageResource(item.comment.rating?.ratingToEmojiRes() ?: R.drawable.ic_emoji_rating_3)
                when {
                    getItem().hasError() -> {
                        statusIcon.setImageResource(R.drawable.psd_error)
                        statusIcon.visibility = VISIBLE
                    }
                    getItem().comment.isLocal() -> {
                        statusIcon.setImageResource(R.drawable.psd_sync_clock)
                        statusIcon.visibility = VISIBLE
                        (statusIcon.drawable as AnimationDrawable).start()
                    }
                    else -> {
                        statusIcon.visibility = GONE
                    }
                }
                setOnClickListener {
                    if (getItem().hasError())
                        onErrorCommentEntryClickListener?.invoke(getItem())
                }
            }
        }

    }

    private class ButtonsHolder(parent: ViewGroup): ViewHolderBase<ButtonsEntry>(parent, R.layout.psd_view_holder_buttons) {

        private val flButtons = itemView.findViewById<FlexboxLayout>(R.id.flButtons)
        override fun bindItem(item: ButtonsEntry) {
            super.bindItem(item)

            item.buttons.forEachIndexed { index, buttonEntry ->
                (flButtons.getChildAt(index) as? TextView)?.apply {
                    val buttonText = buttonEntry.text


                    if (buttonText.length > MAX_SYMBOLS_BEFORE_LEFT_ALIGNMENT) {
                        gravity = Gravity.START
                    }
                    else {
                        gravity - Gravity.CENTER
                    }

                    val frame = background
                    val tint = ConfigUtils.getAccentColor(itemView.context)
                    frame.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
                    setTextColor(ConfigUtils.getAccentColor(itemView.context))


                    when(buttonEntry) {
                        is ButtonEntry.Link -> {
                            text = "$buttonText   "
                            val shareDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_share)!!
                            shareDrawable.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
                            setCompoundDrawablesWithIntrinsicBounds(null, null, shareDrawable, null)
                            setOnClickListener {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(buttonEntry.link) })
                                }
                                catch (exception: Exception) {
                                    exception.printStackTrace()
                                }
                            }

                        }

                        is ButtonEntry.Simple -> {
                            text = buttonText
                            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                            setOnClickListener { item.onButtonClick.invoke(buttonText) }
                        }
                    }

                    visibility = VISIBLE
                }
            }

            repeat(flButtons.childCount - item.buttons.size) {
                flButtons.getChildAt(flButtons.childCount - 1 - it).apply {
                    visibility = GONE
                    setOnClickListener(null)
                }
            }
        }
    }

}
