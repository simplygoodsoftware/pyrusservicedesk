package net.papirus.pyrusservicedesk.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.ServiceDeskActivity
import net.papirus.pyrusservicedesk.utils.getColor

internal class ThemeResolver private constructor(private val context: Context) {

    companion object {
        private var INSTANCE: ThemeResolver? = null
        fun getInstance(): ThemeResolver {
            if (INSTANCE == null)
                INSTANCE = ThemeResolver(PyrusServiceDesk.getInstance().application)
            return INSTANCE!!
        }
    }

    @ColorInt
    fun getAccentColor(): Int {
        return when{
            ServiceDeskActivity.getStyle().themeColor != null -> ServiceDeskActivity.getStyle().themeColor!!
            else -> getColor(context, R.attr.colorAccent)
        }
    }

    fun getTitle(): String {
        return when{
            !ServiceDeskActivity.getStyle().title.isNullOrEmpty() -> ServiceDeskActivity.getStyle().title!!
            else -> context.resources.getString(R.string.psd_organization_support)
        }
    }

    fun getWelcomeMessage(): String? = ServiceDeskActivity.getStyle().welcomeMessage

    fun getSupportAvatar(): Drawable {
        return when {
            ServiceDeskActivity.getStyle().supportAvatar != null -> ServiceDeskActivity.getStyle().supportAvatar!!
            else -> ContextCompat.getDrawable(context, R.drawable.psd_error)!!
        }
    }
}