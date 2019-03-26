package net.papirus.pyrusservicedesk

import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import net.papirus.pyrusservicedesk.ServiceDeskConfiguration.Builder
import net.papirus.pyrusservicedesk.utils.isTablet

private const val KEY_USER_NAME = "ServiceDeskConfiguration_KEY_USER_NAME"
private const val KEY_TITLE = "ServiceDeskConfiguration_KEY_TITLE"
private const val KEY_WELCOME_MESSAGE = "ServiceDeskConfiguration_KEY_WELCOME_MESSAGE"
private const val KEY_THEME_COLOR = "ServiceDeskConfiguration_KEY_THEME_COLOR"
private const val KEY_SUPPORT_AVATAR = "ServiceDeskConfiguration_KEY_SUPPORT_AVATAR"

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
            PyrusServiceDesk.forceConfiguration(
                ServiceDeskConfiguration().apply {
                    userName = bundle.getString(KEY_USER_NAME)
                    title = bundle.getString(KEY_TITLE)
                    welcomeMessage = bundle.getString(KEY_WELCOME_MESSAGE)
                    themeColor = bundle.getInt(KEY_THEME_COLOR).let { if (it == 0) null else it }
                    supportAvatar = bundle.getInt(KEY_SUPPORT_AVATAR).let { if (it == 0) null else it }
                }
            )
        }
    }

    class Builder {
        private var configuration = ServiceDeskConfiguration()

        fun setChatTitle(title: String): Builder {
            configuration.title = title
            return this
        }

        fun setWelcomeMessage(message: String): Builder {
            configuration.welcomeMessage = message
            return this
        }

        fun setThemeColor(@ColorInt color: Int): Builder {
            configuration.themeColor = color
            return this
        }

        fun setAvatarForSupport(@DrawableRes iconResId: Int): Builder {
            configuration.supportAvatar = iconResId
            return this
        }

        fun setUserName(userName: String): Builder {
            configuration.userName = userName
            return this
        }

        fun build() = configuration
    }
}