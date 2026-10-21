package com.roberrini.sundial.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.ColorInt
import kotlin.math.abs

/**
 * Draws the moon's disc with the lit part shaped as it actually appears: the terminator
 * is the ellipse whose horizontal semi-axis is r·(1 − 2·illumination), so the shape runs
 * from a thin crescent through a straight-edged half to a nearly full disc.
 */
object MoonRenderer {

    fun render(
        sizePx: Int,
        illumination: Double,
        waning: Boolean,
        @ColorInt lit: Int,
        @ColorInt dark: Int,
    ): Bitmap {
        require(sizePx > 0) { "Size must be positive, got $sizePx" }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centre = sizePx / 2f
        val radius = centre * DISC_FRACTION

        canvas.drawCircle(centre, centre, radius, fill(dark))

        val fraction = illumination.coerceIn(0.0, 1.0).toFloat()
        if (fraction <= NOTHING_LIT) return bitmap

        canvas.save()
        // The waxing shape mirrored: after full, the moon is lit on the other limb.
        if (waning) canvas.scale(-1f, 1f, centre, centre)

        val terminator = radius * (1 - 2 * fraction)
        val disc = RectF(centre - radius, centre - radius, centre + radius, centre + radius)
        val path = Path().apply {
            // Lit limb, from the top round to the bottom.
            arcTo(disc, -90f, 180f)
            if (abs(terminator) < FLAT_TERMINATOR_PX) {
                // Half moon: the terminator is a straight edge, which close() draws for us.
                close()
            } else {
                val oval = RectF(centre - abs(terminator), centre - radius, centre + abs(terminator), centre + radius)
                // Back from bottom to top: round the near side for a crescent, the far side for a gibbous.
                arcTo(oval, 90f, if (terminator > 0) -180f else 180f)
            }
        }
        canvas.drawPath(path, fill(lit))
        canvas.restore()
        return bitmap
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private const val DISC_FRACTION = 0.92f
    private const val NOTHING_LIT = 0.004f
    private const val FLAT_TERMINATOR_PX = 0.5f
}
