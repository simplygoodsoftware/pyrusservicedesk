package net.papirus.pyrusservicedesk

import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import net.papirus.pyrusservicedesk.utils.isTablet

class ServiceDeskTheme internal constructor(internal val isDialogTheme: Boolean){
    internal var title: String? = null
    internal var welcomeMessage: String? = null
    internal var themeColor: Int? = null
    internal var supportAvatar: Drawable? = null

    class Builder {
        private val serviceDeskTheme = ServiceDeskTheme(PyrusServiceDesk.getInstance().application.isTablet())

        fun setChatTitle(title: String): Builder {
            serviceDeskTheme.title = title
            return this
        }

        fun setWelcomeMessage(message: String): Builder {
            serviceDeskTheme.welcomeMessage = message
            return this
        }

        fun setThemeColor(@ColorInt color: Int): Builder {
            serviceDeskTheme.themeColor = color
            return this
        }

        fun setAvatarForSupport(icon: Drawable): Builder {
            serviceDeskTheme.supportAvatar = icon
            return this
        }

        fun build() = serviceDeskTheme
    }
}