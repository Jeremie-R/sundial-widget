package com.roberrini.sundial.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
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
        detailed: Boolean = false,
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
        if (detailed) drawMaria(canvas, path, centre, radius, dark)
        canvas.restore()
        return bitmap
    }

    /**
     * The dark plains and a few craters, clipped to the lit part so they appear only
     * where the sun is actually falling on them.
     */
    private fun drawMaria(canvas: Canvas, litPath: Path, centre: Float, radius: Float, @ColorInt dark: Int) {
        canvas.save()
        canvas.clipPath(litPath)
        val shade = fill(ColorUtils.setAlphaComponent(dark, 0x66))
        for ((dx, dy, r) in MARIA) {
            canvas.drawCircle(centre + dx * radius, centre + dy * radius, r * radius, shade)
        }
        canvas.restore()
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    /** Maria and craters as (x, y, radius) multiples of the disc radius, from the near side's look. */
    private val MARIA = listOf(
        Triple(-0.30f, -0.34f, 0.26f),
        Triple(0.16f, -0.44f, 0.17f),
        Triple(-0.44f, 0.10f, 0.20f),
        Triple(0.30f, 0.28f, 0.22f),
        Triple(-0.06f, 0.46f, 0.14f),
        Triple(0.52f, -0.16f, 0.12f),
    )

    private const val DISC_FRACTION = 0.92f
    private const val NOTHING_LIT = 0.004f
    private const val FLAT_TERMINATOR_PX = 0.5f
}
