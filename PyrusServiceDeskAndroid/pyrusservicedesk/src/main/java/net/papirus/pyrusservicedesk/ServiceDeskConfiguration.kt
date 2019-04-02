package net.papirus.pyrusservicedesk

import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import net.papirus.pyrusservicedesk.ServiceDeskConfiguration.Builder
import net.papirus.pyrusservicedesk.utils.isTablet

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

    internal val isDialogTheme: Boolean = PyrusServiceDesk.getInstance().application.isTablet()

    internal companion object {
        private const val KEY_USER_NAME = "ServiceDeskConfiguration_KEY_USER_NAME"
        private const val KEY_TITLE = "ServiceDeskConfiguration_KEY_TITLE"
        private const val KEY_WELCOME_MESSAGE = "ServiceDeskConfiguration_KEY_WELCOME_MESSAGE"
        private const val KEY_THEME_COLOR = "ServiceDeskConfiguration_KEY_THEME_COLOR"
        private const val KEY_SUPPORT_AVATAR = "ServiceDeskConfiguration_KEY_SUPPORT_AVATAR"

        fun save(bundle: Bundle) {
            with(PyrusServiceDesk.getConfiguration()) {
                bundle.apply {
                    putString(KEY_USER_NAME, userName)
                    putString(KEY_TITLE, title)
                    putString(KEY_WELCOME_MESSAGE, welcomeMessage)
                    if (themeColor != null)
                        putInt(KEY_THEME_COLOR, themeColor!!)
                    if (supportAvatar != null)
                        putInt(KEY_SUPPORT_AVATAR, supportAvatar!!)
                }
            }
        }

        fun restore(bundle: Bundle) {
            if (!bundle.containsKey(KEY_USER_NAME))
                return
            PyrusServiceDesk.setConfiguration(
                ServiceDeskConfiguration().apply {
                    userName = bundle.getString(KEY_USER_NAME)
                    title = bundle.getString(KEY_TITLE)
                    welcomeMessage = bundle.getString(KEY_WELCOME_MESSAGE)
                    themeColor = bundle.getInt(KEY_THEME_COLOR).let {
                        when (it) {
                            0 -> null
                            else -> it
                        }
                    }
                    supportAvatar = bundle.getInt(KEY_SUPPORT_AVATAR).let {
                        when (it) {
                            0 -> null
                            else -> it
                        }
                    }
                }
            )
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
         * Composes [ServiceDeskConfiguration] instance.
         */
        fun build() = configuration
    }
}