package com.pyrus.pyrusservicedesk

import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration.Builder
import com.pyrus.pyrusservicedesk.core.StaticRepository

/**
 * Represents custom settings that can be applied when service desk is started via [PyrusServiceDesk.start]
 * Use [Builder] to perform setup and make an instance]
 */
class ServiceDeskConfiguration internal constructor() {
    internal var userName: String? = null
    internal var title: String? = null
    internal var welcomeMessage: String? = null
    internal var themeColor: Int? = null
    @DrawableRes
    internal var supportAvatar: Int? = null
    internal var mainMenuDelegate: MainMenuDelegate? = null

    internal var mainFontName: String? = null
    internal var userMessageTextBackgroundColor: Int? = null
    internal var userMessageTextColor: Int? = null
    internal var supportMessageTextBackgroundColor: Int? = null
    internal var supportMessageTextColor: Int? = null
    internal var chatTitleTextColor: Int? = null
    internal var headerBackgroundColor: Int? = null
    internal var backButtonColor: Int? = null
    internal var mainBackgroundColor: Int? = null
    internal var fileMenuBackgroundColor: Int? = null
    internal var fileMenuButtonColor: Int? = null
    internal var fileMenuTextColor: Int? = null
    internal var sendButtonColor: Int? = null
    internal var statusBarColor: Int? = null
    internal var forceDarkAllowed: Boolean = false

    internal var trustedUrls: ArrayList<String>? = null

    // TODO
//    internal val isDialogTheme: Boolean = PyrusServiceDesk.get().application.isTablet()
    internal val isDialogTheme: Boolean = false

    internal companion object {
        private const val KEY_USER_NAME = "ServiceDeskConfiguration_KEY_USER_NAME"
        private const val KEY_TITLE = "ServiceDeskConfiguration_KEY_TITLE"
        private const val KEY_WELCOME_MESSAGE = "ServiceDeskConfiguration_KEY_WELCOME_MESSAGE"
        private const val KEY_THEME_COLOR = "ServiceDeskConfiguration_KEY_THEME_COLOR"
        private const val KEY_SUPPORT_AVATAR = "ServiceDeskConfiguration_KEY_SUPPORT_AVATAR"

        private const val KEY_MAIN_FONT_NAME = "ServiceDeskConfiguration_KEY_MAIN_FONT_NAME"
        private const val KEY_USER_MESSAGE_TEXT_BACKGROUND_COLOR = "ServiceDeskConfiguration_KEY_USER_MESSAGE_TEXT_BACKGROUND_COLOR"
        private const val KEY_USER_MESSAGE_TEXT_COLOR = "ServiceDeskConfiguration_KEY_USER_MESSAGE_TEXT_COLOR"
        private const val KEY_SUPPORT_MESSAGE_TEXT_BACKGROUND_COLOR = "ServiceDeskConfiguration_KEY_SUPPORT_MESSAGE_TEXT_BACKGROUND_COLOR"
        private const val KEY_SUPPORT_MESSAGE_TEXT_COLOR = "ServiceDeskConfiguration_KEY_SUPPORT_MESSAGE_TEXT_COLOR"
        private const val KEY_CHAT_TITLE_TEXT_COLOR = "ServiceDeskConfiguration_KEY_CHAT_TITLE_TEXT_COLOR"
        private const val KEY_HEADER_BACKGROUND_COLOR = "ServiceDeskConfiguration_KEY_HEADER_BACKGROUND_COLOR"
        private const val KEY_BACK_BUTTON_COLOR = "ServiceDeskConfiguration_KEY_BACK_BUTTON_COLOR"
        private const val KEY_MAIN_BACKGROUND_COLOR = "ServiceDeskConfiguration_KEY_MAIN_BACKGROUND_COLOR"
        private const val KEY_FILE_MENU_BACKGROUND_COLOR = "ServiceDeskConfiguration_KEY_FILE_MENU_BACKGROUND_COLOR"
        private const val KEY_FILE_MENU_BUTTON_COLOR = "ServiceDeskConfiguration_KEY_FILE_MENU_BUTTON_COLOR"
        private const val KEY_FILE_MENU_TEXT_COLOR = "ServiceDeskConfiguration_KEY_FILE_MENU_TEXT_COLOR"
        private const val KEY_SEND_BUTTON_COLOR = "ServiceDeskConfiguration_KEY_SEND_BUTTON_COLOR"
        private const val KEY_STATUS_BAR_COLOR = "ServiceDeskConfiguration_KEY_STATUS_BAR_COLOR"
        private const val KEY_FORCE_DARK_ALLOWED = "ServiceDeskConfiguration_KKEY_FORCE_DARK_ALLOWED"
        private const val KEY_TRUSTED_URLS = "ServiceDeskConfiguration_KEY_TRUSTED_URLS"

        fun save(bundle: Bundle) {
            with(StaticRepository.getConfiguration()) {
                bundle.apply {
                    putString(KEY_USER_NAME, userName)
                    putString(KEY_TITLE, title)
                    putString(KEY_WELCOME_MESSAGE, welcomeMessage)
                    if (themeColor != null)
                        putInt(KEY_THEME_COLOR, themeColor!!)
                    if (supportAvatar != null)
                        putInt(KEY_SUPPORT_AVATAR, supportAvatar!!)

                    putString(KEY_MAIN_FONT_NAME, mainFontName)
                    userMessageTextBackgroundColor?.let { putInt(KEY_USER_MESSAGE_TEXT_BACKGROUND_COLOR, it) }
                    userMessageTextColor?.let { putInt(KEY_USER_MESSAGE_TEXT_COLOR, it) }
                    supportMessageTextBackgroundColor?.let { putInt(KEY_SUPPORT_MESSAGE_TEXT_BACKGROUND_COLOR, it) }
                    supportMessageTextColor?.let { putInt(KEY_SUPPORT_MESSAGE_TEXT_COLOR, it) }
                    chatTitleTextColor?.let { putInt(KEY_CHAT_TITLE_TEXT_COLOR, it) }
                    headerBackgroundColor?.let { putInt(KEY_HEADER_BACKGROUND_COLOR, it) }
                    backButtonColor?.let { putInt(KEY_BACK_BUTTON_COLOR, it) }
                    mainBackgroundColor?.let { putInt(KEY_MAIN_BACKGROUND_COLOR, it) }
                    fileMenuBackgroundColor?.let { putInt(KEY_FILE_MENU_BACKGROUND_COLOR, it) }
                    fileMenuButtonColor?.let { putInt(KEY_FILE_MENU_BUTTON_COLOR, it) }
                    fileMenuTextColor?.let { putInt(KEY_FILE_MENU_TEXT_COLOR, it) }
                    sendButtonColor?.let { putInt(KEY_SEND_BUTTON_COLOR, it) }
                    statusBarColor?.let { putInt(KEY_STATUS_BAR_COLOR, it) }
                    trustedUrls?.let { putStringArrayList(KEY_TRUSTED_URLS, it) }
                    putBoolean(KEY_FORCE_DARK_ALLOWED, forceDarkAllowed)
                }
            }
        }

        fun restore(bundle: Bundle) {
            if (!bundle.containsKey(KEY_USER_NAME))
                return
            StaticRepository.setConfiguration(
                ServiceDeskConfiguration().apply {
                    userName = bundle.getString(KEY_USER_NAME)
                    title = bundle.getString(KEY_TITLE)
                    welcomeMessage = bundle.getString(KEY_WELCOME_MESSAGE)
                    themeColor = bundle.getNullableInt(KEY_THEME_COLOR)
                    supportAvatar = bundle.getNullableInt(KEY_SUPPORT_AVATAR)

                    mainFontName = bundle.getString(KEY_MAIN_FONT_NAME)
                    userMessageTextBackgroundColor = bundle.getNullableInt(KEY_USER_MESSAGE_TEXT_BACKGROUND_COLOR)
                    userMessageTextColor = bundle.getNullableInt(KEY_USER_MESSAGE_TEXT_COLOR)
                    supportMessageTextBackgroundColor = bundle.getNullableInt(KEY_SUPPORT_MESSAGE_TEXT_BACKGROUND_COLOR)
                    supportMessageTextColor = bundle.getNullableInt(KEY_SUPPORT_MESSAGE_TEXT_COLOR)
                    chatTitleTextColor = bundle.getNullableInt(KEY_CHAT_TITLE_TEXT_COLOR)
                    headerBackgroundColor = bundle.getNullableInt(KEY_HEADER_BACKGROUND_COLOR)
                    backButtonColor = bundle.getNullableInt(KEY_BACK_BUTTON_COLOR)
                    mainBackgroundColor = bundle.getNullableInt(KEY_MAIN_BACKGROUND_COLOR)
                    fileMenuBackgroundColor = bundle.getNullableInt(KEY_FILE_MENU_BACKGROUND_COLOR)
                    fileMenuTextColor = bundle.getNullableInt(KEY_FILE_MENU_TEXT_COLOR)
                    fileMenuButtonColor = bundle.getNullableInt(KEY_FILE_MENU_BUTTON_COLOR)
                    sendButtonColor = bundle.getNullableInt(KEY_SEND_BUTTON_COLOR)
                    statusBarColor = bundle.getNullableInt(KEY_STATUS_BAR_COLOR)
                    forceDarkAllowed = bundle.getBoolean(KEY_FORCE_DARK_ALLOWED, false)
                }
            )
        }

        private fun Bundle.getNullableInt(key: String): Int? {
            return getInt(key).let {
                when (it) {
                    0 -> null
                    else -> it
                }
            }
        }
    }

    /**
     * Builder for composing custom [ServiceDeskConfiguration] instance.
     */
    class Builder {
        private var configuration = ServiceDeskConfiguration()

        /**
         * Assigns the title text of the comment feed.
         * "Support" is used by default
         *
         * @param title text to be shown
         */
        fun setChatTitle(title: String): Builder {
            configuration.title = title
            return this
        }

        /**
         * Assigns the text of the message that is used as first message of the comment feed.
         * If not assigned, welcome message will be omitted.
         *
         * @param message text of the welcome message
         */
        fun setWelcomeMessage(message: String): Builder {
            configuration.welcomeMessage = message
            return this
        }

        /**
         * Assigns the accent color of the service desk module.
         * Default value is #008C8C
         *
         * @param color int representation of the color to be applied.
         */
        fun setThemeColor(@ColorInt color: Int): Builder {
            configuration.themeColor = color
            return this
        }

        /**
         * Assigns the drawable resource id that is used as placeholder for the avatar of the supporting person.
         * By default chat bubble on the accent color background is drawn.
         *
         * @param iconResId of the drawable resource
         */
        fun setAvatarForSupport(@DrawableRes iconResId: Int): Builder {
            configuration.supportAvatar = iconResId
            return this
        }

        /**
         * Assigns the name of the user who appeals to support.
         * "Guest" is used by default.
         *
         * @param userName name of the user to be applied
         */
        fun setUserName(userName: String): Builder {
            configuration.userName = userName
            return this
        }

        /**
         * Set your own chat menu instead of the standard menu.
         *
         * @param mainMenuDelegate Menu interaction interface.
         */
        fun setChatMenuDelegate(mainMenuDelegate: MainMenuDelegate): Builder {
            configuration.mainMenuDelegate = mainMenuDelegate
            return this
        }

        /**
         * The custom font family.
         *
         * @param fontName Android font name.
         */
        fun setFont(fontName: String): Builder {
            configuration.mainFontName = fontName
            return this
        }

        /**
         * The custom background color for user's messages. The default value is equal to themeColor.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setUserMessageBackgroundColor(@ColorRes color: Int): Builder {
            configuration.userMessageTextBackgroundColor = color
            return this
        }

        /**
         * The custom text color for user's messages.
         * If was not settled, this color will be automatically calculated according to message background view color.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setUserTextColor(@ColorRes color: Int): Builder {
            configuration.userMessageTextColor = color
            return this
        }

        /**
         * The custom background color for support's messages.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setSupportMessageBackgroundColor(@ColorRes color: Int): Builder {
            configuration.supportMessageTextBackgroundColor = color
            return this
        }

        /**
         * The custom text color for support's messages. Default value is is UIColor.label.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setSupportTextColor(@ColorRes color: Int): Builder {
            configuration.supportMessageTextColor = color
            return this
        }

        /**
         * The custom color for toolbar title.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setChatTitleColor(@ColorRes color: Int): Builder {
            configuration.chatTitleTextColor = color
            return this
        }

        /**
         * The custom color of toolbar.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setToolbarColor(@ColorRes color: Int): Builder {
            configuration.headerBackgroundColor = color
            return this
        }

        /**
         * The custom color of back button tint.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setToolbarButtonColor(@ColorRes color: Int): Builder {
            configuration.backButtonColor = color
            return this
        }

        /**
         * The custom color for chat background.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setBackgroundColor(@ColorRes color: Int): Builder {
            configuration.mainBackgroundColor = color
            return this
        }

        /**
         * The custom background color for menu for attachment choosing.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setAttachmentMenuBackgroundColor(@ColorRes color: Int): Builder {
            configuration.fileMenuBackgroundColor = color
            return this
        }

        /**
         * The custom text color for menu for attachment choosing.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setAttachmentMenuTextColor(@ColorRes color: Int): Builder {
            configuration.fileMenuTextColor = color
            return this
        }

        /**
         * The custom color for button for attachment menu.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setAttachmentMenuButtonColor(@ColorRes color: Int): Builder {
            configuration.fileMenuButtonColor = color
            return this
        }

        /**
         * The custom color for button for sending message.
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setSendButtonColor(@ColorRes color: Int): Builder {
            configuration.sendButtonColor = color
            return this
        }

        /**
         * The custom color of the device’s status bar.
         * Color will be set if android api is greater than 21 (Lollipop 5.1).
         * For a dark theme use the color resource from the directory with the "night" qualifier.
         *
         * @param color Id of color resource.
         */
        fun setStatusBarColor(@ColorRes color: Int): Builder {
            configuration.statusBarColor = color
            return this
        }

        /**
         * Sets whether or not to allow force dark.
         *
         * Setting this to false will disable the auto-dark feature.
         * Setting this to true will allow this view to be automatically made dark.
         *
         * False by default.
         *
         * @param forceDarkAllowed Force dark state.
         */
        fun setForceDarkAllowed(forceDarkAllowed: Boolean): Builder {
            configuration.forceDarkAllowed = forceDarkAllowed
            return this
        }

        /**
         * Defines the set of urls that are considered as safe.
         * Warning dialog will not be shown when user clicks on that links in comment
         */
        fun setTrustedUrls(urls: Collection<String>): Builder {
            configuration.trustedUrls = ArrayList(urls)
            return this
        }

        /**
         * Composes [ServiceDeskConfiguration] instance.
         */
        fun build() = configuration
    }
}