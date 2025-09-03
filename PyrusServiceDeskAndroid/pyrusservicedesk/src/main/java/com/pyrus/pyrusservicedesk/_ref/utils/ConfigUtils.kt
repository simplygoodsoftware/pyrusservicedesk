package com.pyrus.pyrusservicedesk._ref.utils

import android.app.PendingIntent
import android.content.*
import android.graphics.*
import android.graphics.drawable.*
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat.getColor
import com.pyrus.pyrusservicedesk.*
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.MultichatButtons
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.utils.PREFERENCE_KEY_USER_ID
import java.util.UUID

internal class ConfigUtils{

    companion object {

        private const val SCALE_SUPPORT_ICON_DEFAULT = .5f

        /**
         * Provides accent color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        @JvmStatic
        fun getAccentColor(context: Context): Int {
            return when {
                StaticRepository.getConfiguration().themeColor != null -> StaticRepository.getConfiguration().themeColor!!
                else -> getColorByAttrId(context, androidx.appcompat.R.attr.colorAccent)
            }
        }
        @ColorInt
        fun getSecondAccentColor(context: Context): Int {
            val overlayColor = context.getColor(R.color.psd_color_blue_white_01)
            val accentColor = getAccentColor(context)

            val overlayAlpha = Color.alpha(overlayColor) / 255f
            val originalAlpha = 1 - overlayAlpha

            val red = (Color.red(overlayColor) * overlayAlpha + Color.red(accentColor) * originalAlpha).toInt()
            val green = (Color.green(overlayColor) * overlayAlpha + Color.green(accentColor) * originalAlpha).toInt()
            val blue = (Color.blue(overlayColor) * overlayAlpha + Color.blue(accentColor) * originalAlpha).toInt()
            return Color.rgb(red, green, blue)
        }

        /**
         * Provides main font Typeface taking [StaticRepository.CONFIGURATION] into account.
         */
        fun getMainFontTypeface(): Typeface? {
            StaticRepository.getConfiguration().mainFontName?.let {
                return Typeface.create(it, Typeface.NORMAL)
            }
            return null
        }

        /**
         * Provides main bold font Typeface taking [StaticRepository.CONFIGURATION] into account.
         */
        fun getMainBoldFontTypeface(): Typeface? {
            StaticRepository.getConfiguration().mainFontName?.let {
                return Typeface.create(it, Typeface.BOLD)
            }
            return null
        }

        /**
         * Provides user message text background color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getUserMessageTextBackgroundColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().userMessageTextBackgroundColor
                ?:                return getAccentColor(context)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides user message text color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         * @param backgroundColor Background color of text.
         *
         * If no custom text color is set, the color is calculated based on the brightness of the [backgroundColor].
         * If the [backgroundColor] is bright, the text color will be dark, and vice versa, if the [backgroundColor] is light, the text will be light.
         */
        @ColorInt
        fun getUserMessageTextColor(context: Context, @ColorInt backgroundColor: Int): Int {
            val colorRes = StaticRepository.getConfiguration().userMessageTextColor
                ?:                return getTextColorOnBackground(context, backgroundColor)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides support message text background color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getSupportMessageTextBackgroundColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().supportMessageTextBackgroundColor
                ?: return getColor(context, R.color.psd_comment_inbound_bg)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides support message text color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         * @param backgroundColor Background color of text.
         *
         * If no custom text color is set, the color is calculated based on the brightness of the [backgroundColor].
         * If the [backgroundColor] is bright, the text color will be dark, and vice versa, if the [backgroundColor] is light, the text will be light.
         */
        @ColorInt
        fun getSupportMessageTextColor(context: Context, @ColorInt backgroundColor: Int): Int {
            val colorRes = StaticRepository.getConfiguration().supportMessageTextColor
                ?: return getTextColorOnBackground(context, backgroundColor)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides chat title text color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getChatTitleTextColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().chatTitleTextColor
                ?: return getTextColorOnBackground(context, getHeaderBackgroundColor(context))
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides header background color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getHeaderBackgroundColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().headerBackgroundColor
                ?: return getColor(context, R.color.psd_background)
            return getColor(context.applicationContext, colorRes)
        }

        @ColorInt
        fun getLockColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().headerBackgroundColor
                ?: return StaticRepository.getConfiguration().themeColor
                ?: return getColor(context, R.color.psd_background)
            return getColor(context.applicationContext, colorRes)
        }

        @ColorInt
        fun getCanselColor(context: Context): Int {
            val accentColor = StaticRepository.getConfiguration().themeColor
            if (accentColor != null)
                return accentColor
            val colorRes = StaticRepository.getConfiguration().headerBackgroundColor
                ?: return getColor(context, R.color.psd_hint_color)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides back button color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getToolbarButtonColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().backButtonColor
                ?: return getTextColorOnBackground(context, getHeaderBackgroundColor(context))
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides main background color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getMainBackgroundColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().mainBackgroundColor
                ?: return getColor(context, R.color.psd_background)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides no file preview background color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getNoPreviewBackgroundColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().mainBackgroundColor
                ?: return getColor(context, R.color.psd_no_file_preview_background)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides no connection background color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getNoConnectionBackgroundColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().mainBackgroundColor
                ?: return getColor(context, R.color.psd_error_background)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides file menu background color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getFileMenuBackgroundColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().fileMenuBackgroundColor
                ?: return getColor(context, R.color.psd_background)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides file menu button color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getFileMenuButtonColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().fileMenuButtonColor
                ?: return getAccentColor(context)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides status bar color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getStatusBarColor(context: Context): Int? {
            val colorRes = StaticRepository.getConfiguration().statusBarColor ?: return null
            return getColor(context.applicationContext, colorRes)

        }

        /**
         * Provides file menu text color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getFileMenuTextColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().fileMenuTextColor
                ?: return getTextColorOnBackground(context, getFileMenuBackgroundColor(context))
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides send button color taking [StaticRepository.CONFIGURATION] into account.
         * @param context Activity context.
         */
        @ColorInt
        fun getSendButtonColor(context: Context): Int {
            val colorRes = StaticRepository.getConfiguration().sendButtonColor
                ?: return getAccentColor(context)
            return getColor(context.applicationContext, colorRes)
        }

        /**
         * Provides secondary color on main background.
         * @param context Activity context.
         */
        @ColorInt
        @JvmStatic
        fun getSecondaryColorOnMainBackground(context: Context): Int {
            return getSecondaryColorOnBackground(getMainBackgroundColor(context))
        }

        /**
         * Provides input hint text color.
         * @param context Activity context.
         */
        @ColorInt
        fun getInputTextColor(context: Context): Int {
            return getTextColorOnBackground(context, getMainBackgroundColor(context))
        }

        /**
         * Provides chat title taking [StaticRepository.CONFIGURATION] into account
         */
        fun getTitle(context: Context, subject: String? = null): String {
            val title = StaticRepository.getConfiguration().title
            return when{
                !subject.isNullOrBlank() -> subject
                !title.isNullOrEmpty() -> title
                else -> context.resources.getString(R.string.psd_organization_support)
            }
        }

        /**
         * Provides chat welcome message taking [StaticRepository.CONFIGURATION] into account
         */
        fun getWelcomeMessage(): String? = StaticRepository.getConfiguration().welcomeMessage

        fun getTrustedUrls() = StaticRepository.getConfiguration().trustedUrls

        /**
         * Provides avatar placeholder taking [StaticRepository.CONFIGURATION] into account
         */
        fun getSupportAvatar(context: Context): Drawable {
            return when {
                StaticRepository.getConfiguration().supportAvatar != null ->
                    try {
                        AppCompatResources.getDrawable(context, StaticRepository.getConfiguration().supportAvatar!!)!!.circle(context)
                    }
                    catch (ex: Exception) {
                        makeSupportAvatar(
                            context,
                            AppCompatResources.getDrawable(context, R.drawable.psd_support_avatar)!!
                        )
                    }
                else -> makeSupportAvatar(context, AppCompatResources.getDrawable(context, R.drawable.psd_support_avatar)!!)
            }
        }

        fun getAuthorAvatar(context: Context): Drawable {
            return makeSupportAvatar(context, AppCompatResources.getDrawable(context, R.drawable.psd_support_avatar)!!)
        }

        /**
         * Provides user name taking [StaticRepository.CONFIGURATION] into account
         */
        fun getAuthorName(resourceManager: AppResourceManager): String {
            return when {
                !StaticRepository.getConfiguration().userName.isNullOrBlank() ->
                    StaticRepository.getConfiguration().userName!!
                else -> {
                    resourceManager.getString(R.string.psd_guest)
                }
            }
        }

        /**
         * Provides multichat buttons taking [StaticRepository.CONFIGURATION] into account
         */
        fun getMultichatButtons(): MultichatButtons? {
            return StaticRepository.getConfiguration().multichatButtons
        }

        fun getChangeUsersIntent(): PendingIntent? {
            return StaticRepository.getConfiguration().changeUsersIntent
        }

        /**
         * Provides userId. [preference] is used for storing generated user id.
         */
        fun getInstanceId(preference: SharedPreferences): String {
            return when {
                preference.contains(PREFERENCE_KEY_USER_ID) -> preference.getString(PREFERENCE_KEY_USER_ID, "")!!
                else -> {
                    val userId = UUID.randomUUID().toString()
                    preference.edit().putString(PREFERENCE_KEY_USER_ID, userId).apply()
                    userId
                }
            }
        }

        private fun makeSupportAvatar(context: Context, drawable: Drawable): Drawable {
            val bmp = Bitmap.createBitmap(drawable.intrinsicHeight, drawable.intrinsicWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint()
            paint.color = getAccentColor(context)
            paint.isAntiAlias = true
            canvas.drawCircle(canvas.width / 2f, canvas.height / 2f, canvas.height / 2f, paint)

            val icon = Bitmap.createBitmap(drawable.intrinsicHeight, drawable.intrinsicWidth, Bitmap.Config.ARGB_8888)
            val iconCanvas = Canvas(icon)
            drawable.setBounds(0, 0, iconCanvas.width, iconCanvas.height)
            drawable.draw(iconCanvas)

            paint.colorFilter = PorterDuffColorFilter(getTextColorOnBackground(context, paint.color), PorterDuff.Mode.SRC_IN)
            val iconTranslate = (canvas.width - canvas.width*SCALE_SUPPORT_ICON_DEFAULT) / 2

            canvas.drawBitmap(
                icon,
                Matrix().apply {
                    postScale(SCALE_SUPPORT_ICON_DEFAULT, SCALE_SUPPORT_ICON_DEFAULT)
                    postTranslate(iconTranslate, iconTranslate)
                },
                paint)


            return BitmapDrawable(context.resources, bmp)
        }
    }
}

