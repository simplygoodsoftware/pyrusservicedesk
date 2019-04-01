package net.papirus.pyrusservicedesk.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import com.squareup.picasso.Transformation

/**
 * Picasa [Transformation] implementation that uses [circle] for
 * transforming image.
 */
internal val CIRCLE_TRANSFORMATION = object : Transformation {
    override fun key(): String = "Circle"
    override fun transform(source: Bitmap): Bitmap = source.circle()
}

/**
 * Makes circled bitmap from the source. It makes new square bitmap 96x96 px from the center of the source
 * and inscribe it into an oval. If the source image is less than 96x96, it will be stretched.
 * @return new 96x96 px bitmap with circle image
 */
internal fun Bitmap.circle(): Bitmap {
    val dimension = this.width
    val scaled = ThumbnailUtils.extractThumbnail(this , dimension, dimension, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
    val output = Bitmap.createBitmap(
        scaled.width,
        scaled.height, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    val color = Color.RED
    val paint = Paint()
    val rect = Rect(0, 0, scaled.width, scaled.height)
    val rectF = RectF(rect)
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawOval(rectF, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaled, rect, rect, paint)
    scaled.recycle()
    return output
}

/**
 * Makes drawable circle.
 * @return new drawable instance that has circle shape
 */
internal fun Drawable.circle(context: Context): Drawable {
    val out = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    this.mutate().run {
        setBounds(0, 0, out.width, out.height)
        draw(Canvas(out))
    }
    return BitmapDrawable(context.resources, out.circle())
}
