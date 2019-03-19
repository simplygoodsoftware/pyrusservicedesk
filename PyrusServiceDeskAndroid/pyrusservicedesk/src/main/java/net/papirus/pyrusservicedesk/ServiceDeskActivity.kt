package net.papirus.pyrusservicedesk

import android.content.Intent
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import net.papirus.pyrusservicedesk.ui.usecases.ticket.TicketActivity
import net.papirus.pyrusservicedesk.ui.usecases.tickets.TicketsActivity

class ServiceDeskActivity private constructor(){
    companion object {
        private var STYLE: ServiceDeskActivity.ServiceDeskStyle? = null
        internal fun getStyle(): ServiceDeskStyle {
            if (STYLE == null)
                STYLE = ServiceDeskStyle()
            return STYLE!!
        }
        private fun createIntent(): Intent {
            return when{
                PyrusServiceDesk.getInstance().enableFeedUi -> TicketActivity.getLaunchIntent()
                else -> TicketsActivity.getLaunchIntent()
            }
        }
    }
    class Builder{
        private var style:ServiceDeskStyle? = null
        fun setStyle(style: ServiceDeskActivity.ServiceDeskStyle): Builder{
            this.style = style
            return this
        }
        fun build(): Intent{
            if (style != null)
                STYLE = style
            return createIntent()
        }
    }

    class StyleBuilder {
        private val serviceDeskStyle = ServiceDeskActivity.ServiceDeskStyle()
        fun setShowAsDialog(asDialog: Boolean): StyleBuilder {
            serviceDeskStyle.isDialogStyle = asDialog
            return this
        }

        fun setTitle(title: String): StyleBuilder {
            serviceDeskStyle.title = title
            return this
        }

        fun setWelcomeMessage(message: String): StyleBuilder {
            serviceDeskStyle.welcomeMessage = message
            return this
        }

        fun setThemeColor(@ColorInt color: Int): StyleBuilder {
            serviceDeskStyle.themeColor = color
            return this
        }

        fun setDrawableForSupport(icon: Drawable): StyleBuilder {
            serviceDeskStyle.supportAvatar = icon
            return this
        }

        fun build() = serviceDeskStyle
    }

    class ServiceDeskStyle internal constructor(){
        internal var isDialogStyle: Boolean = false
        internal var title: String? = null
        internal var welcomeMessage: String? = null
        internal var themeColor: Int? = null
        internal var supportAvatar: Drawable? = null
    }
}