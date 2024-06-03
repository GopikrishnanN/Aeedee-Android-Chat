package com.prng.aeedee_android_chat.util

import android.graphics.*
import android.graphics.drawable.Drawable

class SideCurvedDrawable (private val radius: Float) : Drawable() {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
    }

    private val path = Path()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        bounds.let {
            path.reset()
            val radii = floatArrayOf(
                radius, radius,   // Top-left corner
                radius, radius,   // Top-right corner
                radius, radius,   // Bottom-right corner (no radius)
                0f, 0f            // Bottom-left corner
            )
            path.addRoundRect(RectF(it), radii, Path.Direction.CW)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}