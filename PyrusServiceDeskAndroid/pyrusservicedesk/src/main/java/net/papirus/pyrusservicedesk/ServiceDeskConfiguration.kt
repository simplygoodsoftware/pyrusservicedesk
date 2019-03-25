package net.papirus.pyrusservicedesk

import android.graphics.drawable.BitmapDrawable
import android.support.annotation.ColorInt
import net.papirus.pyrusservicedesk.ServiceDeskConfiguration.Builder
import net.papirus.pyrusservicedesk.utils.isTablet

/**
 * Represents custom settings that can be applied when service desk is started via [PyrusServiceDesk.start]
 * Use [Builder] to perform setup and make an instance]
 */
class ServiceDeskConfiguration internal constructor(){
    internal var userName: String? = null
    internal var title: String? = null
    internal var welcomeMessage: String? = null
    internal var themeColor: Int? = null
    internal var supportAvatar: BitmapDrawable? = null

    internal val isDialogTheme: Boolean = PyrusServiceDesk.getInstance().application.isTablet()

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

        fun setAvatarForSupport(icon: BitmapDrawable): Builder {
            configuration.supportAvatar = icon
            return this
        }

        fun setUserName(userName: String): Builder {
            configuration.userName = userName
            return this
        }

        fun build() = configuration
    }
}