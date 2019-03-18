package net.papirus.pyrusservicedesk

import android.content.Intent
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
        fun showAsDialog(asDialog: Boolean): StyleBuilder {
            serviceDeskStyle.isDialogStyle = asDialog
            return this
        }
        fun build() = serviceDeskStyle
    }

    class ServiceDeskStyle internal constructor(){
        internal var isDialogStyle: Boolean = false
    }
}