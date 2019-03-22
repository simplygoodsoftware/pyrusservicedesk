package net.papirus.pyrusservicedesk

import android.graphics.drawable.BitmapDrawable
import android.support.annotation.ColorInt
import net.papirus.pyrusservicedesk.utils.isTablet

class ServiceDeskConfigure internal constructor(
    internal var userName: String? = null,
    internal val isDialogTheme: Boolean,
    internal var title: String? = null,
    internal var welcomeMessage: String? = null,
    internal var themeColor: Int? = null,
    internal var supportAvatar: BitmapDrawable? = null){

    class Builder {
        private var userName: String? = null
        private var title: String? = null
        private var welcomeMessage: String? = null
        private var themeColor: Int? = null
        private var supportAvatar: BitmapDrawable? = null

        fun setChatTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setWelcomeMessage(message: String): Builder {
            welcomeMessage = message
            return this
        }

        fun setThemeColor(@ColorInt color: Int): Builder {
            themeColor = color
            return this
        }

        fun setAvatarForSupport(icon: BitmapDrawable): Builder {
            supportAvatar = icon
            return this
        }

        fun setUserName(userName: String) {
            this.userName = userName
        }

        fun build() {
            ServiceDeskConfigure(
                userName,
                PyrusServiceDesk.getInstance().application.isTablet(),
                title,
                welcomeMessage,
                themeColor,
                supportAvatar
            )
        }
    }
}