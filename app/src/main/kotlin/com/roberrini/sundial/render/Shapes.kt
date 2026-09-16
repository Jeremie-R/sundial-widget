package com.roberrini.sundial.render

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Path builders for the background shapes and sparkles. Same construction as the design
 * lab: a star polygon whose corners are smoothed with quadratic curves, so the Kotlin
 * output matches what was chosen on the page.
 */
object Shapes {

    fun background(shape: Shape, cx: Float, cy: Float, r: Float): Path? = when (shape) {
        Shape.NONE -> null
        Shape.CIRCLE -> Path().apply { addCircle(cx, cy, r * 0.98f, Path.Direction.CW) }
        Shape.SQUIRCLE -> Path().apply {
            val half = r * 0.96f
            addRoundRect(RectF(cx - half, cy - half, cx + half, cy + half), r * 0.54f, r * 0.54f, Path.Direction.CW)
        }
        Shape.COOKIE_12 -> star(cx, cy, r, r * 0.80f, 12, 1f)
        Shape.COOKIE_9 -> star(cx, cy, r, r * 0.76f, 9, 1f)
        Shape.COOKIE_7 -> star(cx, cy, r, r * 0.72f, 7, 1f)
        Shape.COOKIE_4 -> star(cx, cy, r, r * 0.56f, 4, 1f)
        Shape.CLOVER_8 -> star(cx, cy, r, r * 0.62f, 8, 1f)
        Shape.SUNNY -> star(cx, cy, r, r * 0.84f, 8, 0.45f)
        Shape.BURST -> star(cx, cy, r, r * 0.74f, 12, 0.08f)
    }

    /** Four-point sparkle. */
    fun sparkle(cx: Float, cy: Float, size: Float): Path = star(cx, cy, size, size * 0.42f, 4, 0.5f)

    /**
     * Star polygon with [points] outer vertices at [rOut] and inner vertices at [rIn], with
     * each corner replaced by a quadratic curve spanning [rounding] × half of each adjacent
     * edge (1f = fully smooth). One point faces up.
     */
    fun star(cx: Float, cy: Float, rOut: Float, rIn: Float, points: Int, rounding: Float): Path {
        val count = points * 2
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        for (i in 0 until count) {
            val angle = -PI / 2 + PI * i / points
            val r = if (i % 2 == 0) rOut else rIn
            xs[i] = cx + r * cos(angle).toFloat()
            ys[i] = cy + r * sin(angle).toFloat()
        }
        val path = Path()
        if (rounding <= 0f) {
            path.moveTo(xs[0], ys[0])
            for (i in 1 until count) path.lineTo(xs[i], ys[i])
            path.close()
            return path
        }
        val k = rounding * 0.5f
        for (i in 0 until count) {
            val p = (i - 1 + count) % count
            val n = (i + 1) % count
            val ax = xs[i] + (xs[p] - xs[i]) * k
            val ay = ys[i] + (ys[p] - ys[i]) * k
            val bx = xs[i] + (xs[n] - xs[i]) * k
            val by = ys[i] + (ys[n] - ys[i]) * k
            if (i == 0) path.moveTo(ax, ay) else path.lineTo(ax, ay)
            path.quadTo(xs[i], ys[i], bx, by)
        }
        path.close()
        return path
    }
}
