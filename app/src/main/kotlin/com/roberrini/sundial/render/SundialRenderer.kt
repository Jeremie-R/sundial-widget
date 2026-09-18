package com.roberrini.sundial.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils
import com.roberrini.sundial.sun.DaySpan
import com.roberrini.sundial.sun.SunTimes
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the sundial face into a [Bitmap]. Pure Canvas code with no Android component
 * dependencies, so it can be unit-tested, previewed in the settings screen, and rendered
 * for the widget from the same function.
 *
 * The face is a 24h dial: midnight at the bottom, noon at the top, time running
 * clockwise. Today's daylight is drawn from sunrise to sunset; a thinner bar extends it
 * to astronomical dawn and dusk, fading through the civil, nautical and astronomical
 * twilights; the night is left blank. The sun sits on the same circle at the current
 * time, filled while above the horizon. The centre is left empty for the time and date
 * the widget layout overlays (or [TextOverlay] paints, for previews).
 *
 * Geometry is expressed as fractions of the shorter bitmap edge, matching the design
 * lab's 200-unit box, so every size renders the same picture.
 */
object SundialRenderer {

    /** Colours and weight the time/date text should use for a look. */
    data class TextStyle(@param:ColorInt val time: Int, @param:ColorInt val date: Int, val heavy: Boolean)

    /** Text painted into the bitmap itself; only for previews, the widget uses TextClocks. */
    data class TextOverlay(val time: String, val date: String)

    fun textStyle(look: Look, c: SundialColors): TextStyle = when (look) {
        Look.PILLOW -> TextStyle(c.primary, c.primary, heavy = true)
        Look.BOLD -> TextStyle(c.onSurface, c.onSurfaceVariant, heavy = true)
        Look.LINE -> TextStyle(c.primary, c.onSurfaceVariant, heavy = false)
        Look.HIGH_CONTRAST -> TextStyle(c.ink, c.ink, heavy = true)
        Look.BEADS -> TextStyle(c.onSecondaryContainer, c.onSurfaceVariant, heavy = true)
    }

    /**
     * @param progress fraction of the 24h day elapsed: 0f = midnight, 0.5f = noon, 1f = next midnight
     * @param sunTimes today's daylight and twilight spans, in the same 0f..1f units
     */
    fun render(
        width: Int,
        height: Int,
        @FloatRange(from = 0.0, to = 1.0) progress: Float,
        sunTimes: SunTimes,
        colors: SundialColors,
        style: SundialStyle,
        text: TextOverlay? = null,
    ): Bitmap {
        require(width > 0 && height > 0) { "Bitmap size must be positive, got ${width}x$height" }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Face(Canvas(bitmap), width.toFloat(), height.toFloat(), progress.coerceIn(0f, 1f), sunTimes, colors, style).draw(text)
        return bitmap
    }

    private class Face(
        val canvas: Canvas,
        width: Float,
        height: Float,
        val t: Float,
        val sun: SunTimes,
        val c: SundialColors,
        val style: SundialStyle,
    ) {
        val size = min(width, height)
        val cx = width / 2f
        val cy = height / 2f
        val radius = size * RADIUS
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val look = style.look

        /** What sits behind the dial: the shape's fill, or the surface colour when there is no shape. */
        val background: Int = when (look) {
            Look.PILLOW -> c.primaryContainer
            Look.BOLD -> c.surfaceContainer
            Look.LINE -> c.surface
            Look.HIGH_CONTRAST -> c.ground
            Look.BEADS -> c.secondaryContainer
        }
        val ground: Int = if (style.shape == Shape.NONE) c.surface else background

        fun draw(text: TextOverlay?) {
            drawBackground()
            drawArcs()
            if (style.sparkles) drawSparkles()
            drawSun()
            if (text != null) drawText(text)
        }

        private fun drawBackground() {
            val path = Shapes.background(style.shape, cx, cy, size / 2f) ?: return
            canvas.drawPath(path, fill(background))
            if (look == Look.HIGH_CONTRAST) canvas.drawPath(path, stroke(size * 0.015f, c.ink))
        }

        private fun drawArcs() {
            val daylight = sun.daylight
            when (look) {
                Look.PILLOW -> {
                    drawTwilightBar(size * 0.0125f, c.outline, 0xFF)
                    drawSpan(daylight, stroke(size * 0.095f, c.outline))
                    drawSpan(daylight, stroke(size * 0.07f, ground))
                }
                Look.BOLD -> {
                    drawTwilightBar(size * 0.025f, c.primary, 0x59)
                    drawSpan(daylight, stroke(size * 0.065f, c.primary))
                }
                Look.LINE -> {
                    drawTwilightBar(size * 0.0075f, c.outlineVariant, 0xFF)
                    drawSpan(daylight, stroke(size * 0.015f, c.primary))
                    val tick = stroke(size * 0.0075f, c.outline)
                    for (mark in floatArrayOf(0.25f, 0.75f)) {
                        val (x, y) = point(mark)
                        canvas.drawLine(x - size * 0.03f, y, x + size * 0.03f, y, tick)
                    }
                }
                Look.HIGH_CONTRAST -> {
                    // No fade here: contrast beats nuance.
                    drawSpan(sun.astronomical, stroke(size * 0.015f, c.ink))
                    drawSpan(daylight, stroke(size * 0.045f, c.ink))
                }
                Look.BEADS -> {
                    drawBeads(sun.astronomical, 13f / 1440f, size * 0.006f, ColorUtils.setAlphaComponent(c.outline, 0x80), skip = sun.nautical)
                    drawBeads(sun.nautical, 13f / 1440f, size * 0.007f, ColorUtils.setAlphaComponent(c.outline, 0xC0), skip = sun.civil)
                    drawBeads(sun.civil, 13f / 1440f, size * 0.008f, c.outline, skip = daylight)
                    drawBeads(daylight, 30f / 1440f, size * 0.016f, c.primary, skip = null)
                }
            }
        }

        /**
         * The twilight bar: astronomical, nautical and civil spans painted over each other,
         * so the line is faint at the ends and solid (at [alpha]) next to the daylight track.
         */
        private fun drawTwilightBar(width: Float, color: Int, alpha: Int) {
            drawSpan(sun.astronomical, stroke(width, ColorUtils.setAlphaComponent(color, alpha * 2 / 5)))
            drawSpan(sun.nautical, stroke(width, ColorUtils.setAlphaComponent(color, alpha * 7 / 10)))
            drawSpan(sun.civil, stroke(width, ColorUtils.setAlphaComponent(color, alpha)))
        }

        private fun drawBeads(span: DaySpan, step: Float, r: Float, color: Int, skip: DaySpan?) {
            val paint = fill(color)
            val (start, sweep) = startAndSweep(span) ?: return
            var offset = 0f
            while (offset <= sweep + 1e-4f) {
                val tt = (start + offset) % 1f
                if (skip == null || !skip.contains(tt)) {
                    val (x, y) = point(tt)
                    canvas.drawCircle(x, y, r, paint)
                }
                offset += step
            }
        }

        private fun drawSparkles() {
            val color = when (look) {
                Look.LINE -> c.outlineVariant
                Look.HIGH_CONTRAST -> c.ink
                else -> c.outline
            }
            val paint = fill(ColorUtils.setAlphaComponent(color, 0xCC))
            for ((tt, rr, sz) in SPARKLES) {
                if (sun.astronomical.contains(tt)) continue
                val (x, y) = point(tt, radius * rr)
                canvas.drawPath(Shapes.sparkle(x, y, size * sz), paint)
            }
        }

        private fun drawSun() {
            val (sx, sy) = point(t)
            val sunRadius = size * when (look) {
                Look.PILLOW -> 0.0525f
                Look.BOLD -> 0.065f
                Look.LINE -> 0.045f
                Look.HIGH_CONTRAST -> 0.06f
                Look.BEADS -> 0.055f
            }
            // Filled only while above the horizon; below it, the night treatment applies.
            val isNight = !sun.daylight.contains(t)
            if (!isNight) {
                when (look) {
                    Look.PILLOW -> filledSun(sx, sy, sunRadius, c.primary, catchlight = true)
                    Look.BOLD -> {
                        canvas.drawCircle(sx, sy, sunRadius + size * 0.015f, fill(background))
                        canvas.drawCircle(sx, sy, sunRadius, fill(c.tertiary))
                    }
                    Look.LINE -> {
                        canvas.drawCircle(sx, sy, sunRadius, fill(c.surface))
                        canvas.drawCircle(sx, sy, sunRadius, stroke(size * 0.015f, c.primary))
                    }
                    Look.HIGH_CONTRAST -> {
                        canvas.drawCircle(sx, sy, sunRadius + size * 0.015f, fill(c.ground))
                        canvas.drawCircle(sx, sy, sunRadius, fill(c.ink))
                    }
                    Look.BEADS -> filledSun(sx, sy, sunRadius, c.tertiary, catchlight = true)
                }
                return
            }
            when (style.nightSun) {
                NightSun.HIDDEN -> Unit
                NightSun.MOON -> {
                    val mr = sunRadius * 0.85f
                    val moon = Path().apply { addCircle(sx, sy, mr, Path.Direction.CW) }
                    val bite = Path().apply { addCircle(sx + mr * 0.45f, sy - mr * 0.25f, mr * 0.85f, Path.Direction.CW) }
                    moon.op(bite, Path.Op.DIFFERENCE)
                    val color = when (look) {
                        Look.HIGH_CONTRAST -> c.ink
                        Look.PILLOW -> c.primary
                        else -> c.onSurfaceVariant
                    }
                    canvas.drawPath(moon, fill(color))
                }
                NightSun.OUTLINE -> {
                    val (w, color) = when (look) {
                        Look.PILLOW -> 0.0125f to c.outline
                        Look.BOLD -> 0.015f to ColorUtils.setAlphaComponent(c.primary, 0x80)
                        Look.LINE -> 0.0075f to c.outline
                        Look.HIGH_CONTRAST -> 0.015f to c.ink
                        Look.BEADS -> 0.01f to c.outline
                    }
                    canvas.drawCircle(sx, sy, sunRadius * 0.8f, stroke(size * w, color))
                }
            }
        }

        private fun filledSun(x: Float, y: Float, r: Float, color: Int, catchlight: Boolean) {
            canvas.drawCircle(x, y, r, fill(color))
            if (catchlight) canvas.drawCircle(x - r * 0.38f, y - r * 0.38f, r * 0.24f, fill(c.catchlight))
        }

        private fun drawText(text: TextOverlay) {
            val ts = textStyle(look, c)
            val time = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ts.time
                textAlign = Paint.Align.CENTER
                textSize = size * TIME_TEXT
                typeface = Typeface.create(if (ts.heavy) "sans-serif-black" else "sans-serif-medium", Typeface.NORMAL)
            }
            val date = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ts.date
                textAlign = Paint.Align.CENTER
                textSize = size * DATE_TEXT
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            canvas.drawText(text.time, cx, cy + size * 0.06f, time)
            canvas.drawText(text.date, cx, cy + size * 0.155f, date)
        }

        // ---- geometry helpers ----

        private fun point(tt: Float, r: Float = radius): Pair<Float, Float> {
            val angle = Math.toRadians(90.0 + tt * 360.0)
            return (cx + r * cos(angle).toFloat()) to (cy + r * sin(angle).toFloat())
        }

        private fun startAndSweep(span: DaySpan): Pair<Float, Float>? = when (span) {
            DaySpan.None -> null
            DaySpan.All -> 0f to 1f
            is DaySpan.Between -> span.start to ((span.end - span.start + 1f) % 1f)
        }

        private fun drawSpan(span: DaySpan, paint: Paint) {
            when (span) {
                DaySpan.None -> Unit
                DaySpan.All -> canvas.drawOval(oval, paint)
                is DaySpan.Between -> {
                    val (start, sweep) = startAndSweep(span)!!
                    canvas.drawArc(oval, 90f + start * 360f, sweep * 360f, false, paint)
                }
            }
        }

        private fun stroke(strokeWidth: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = strokeWidth
            this.color = color
        }

        private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
    }

    /** Dial radius as a fraction of the shorter edge (64 of the lab's 200 units). */
    private const val RADIUS = 0.32f

    /** Text sizes as fractions of the shorter edge (34 and 12.5 of 200). */
    const val TIME_TEXT = 0.17f
    const val DATE_TEXT = 0.0625f

    /** Night sparkles: (time of day, radius multiplier, size fraction). Skipped if inside the twilight bar. */
    private val SPARKLES = listOf(
        Triple(0.035f, 0.94f, 0.017f), Triple(0.11f, 1.08f, 0.013f), Triple(0.19f, 0.90f, 0.015f),
        Triple(0.82f, 1.06f, 0.013f), Triple(0.90f, 0.92f, 0.017f), Triple(0.965f, 1.10f, 0.012f),
    )
}
