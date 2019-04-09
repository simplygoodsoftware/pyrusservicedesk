package com.pyrus.pyrusservicedesk.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.v7.content.res.AppCompatResources
import android.util.Base64
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import java.security.SecureRandom

internal class ConfigUtils{

    companion object {

        private const val SCALE_SUPPORT_ICON_DEFAULT = .5f

        /**
         * Provides accent color taking [PyrusServiceDesk.CONFIGURATION] into account
         */
        @ColorInt
        fun getAccentColor(activity: Context): Int {
            return when{
                PyrusServiceDesk.getConfiguration().themeColor != null -> PyrusServiceDesk.getConfiguration().themeColor!!
                else -> getColor(activity, R.attr.colorAccent)
            }
        }

        /**
         * Provides chat title taking [PyrusServiceDesk.CONFIGURATION] into account
         */
        fun getTitle(context: Context): String {
            return when{
                !PyrusServiceDesk.getConfiguration().title.isNullOrEmpty() -> PyrusServiceDesk.getConfiguration().title!!
                else -> context.resources.getString(R.string.psd_organization_support)
            }
        }

        /**
         * Provides chat welcome message taking [PyrusServiceDesk.CONFIGURATION] into account
         */
        fun getWelcomeMessage(): String? = PyrusServiceDesk.getConfiguration().welcomeMessage


        /**
         * Provides avatar placeholder taking [PyrusServiceDesk.CONFIGURATION] into account
         */
        fun getSupportAvatar(context: Context): Drawable {
            return when {
                PyrusServiceDesk.getConfiguration().supportAvatar != null ->
                    try {
                        AppCompatResources.getDrawable(context, PyrusServiceDesk.getConfiguration().supportAvatar!!)!!.circle(context)
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

        /**
         * Provides user name taking [PyrusServiceDesk.CONFIGURATION] into account
         */
        fun getUserName(): String {
            return when {
                !PyrusServiceDesk.getConfiguration().userName.isNullOrBlank() ->
                    PyrusServiceDesk.getConfiguration().userName!!
                else -> PyrusServiceDesk.getInstance().application.getString(R.string.psd_guest)
            }
        }

        /**
         * Provides userId. [preference] is used for storing generated user id.
         */
        fun getUserId(preference: SharedPreferences): String {
            return when {
                preference.contains(PREFERENCE_KEY_USER_ID) -> preference.getString(PREFERENCE_KEY_USER_ID, "")
                else -> {
                    val userId = Base64.encodeToString(
                        ByteArray(75).run {
                            SecureRandom().nextBytes(this)
                            this
                        },
                        Base64.NO_WRAP)
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

