package com.coai.uikit.load

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.samin.carerobot.LoadingPage.Arc
import com.samin.carerobot.LoadingPage.ViewSupport

class Loader : ViewSupport() {
    private lateinit var arcs: Array<Arc?>
    private val numberOfArc = 7
    private lateinit var rotates: FloatArray

    override fun initializeObjects() {
        val r = Math.min(width, height) / 2f
        arcs = arrayOfNulls(numberOfArc)
        rotates = FloatArray(numberOfArc)
        val base = r / 2
        var d = base
        arcs[0] = Arc().apply {
            setColor(Color.parseColor("#EFB248"))
            setOval(RectF(center!!.x - d, center!!.y - d, center!!.x + d, center!!.y + d))
            startAngle = 270f
            setSweepAngle(360f)
            setStyle(Paint.Style.STROKE)
            setAlpha((255 * 0.7f).toInt())
            setWidth(r * 0.02f)
        }

        d = base + r / 7
        arcs[1] = Arc().apply {
            setColor(Color.parseColor("#EF8683"))
            setOval(RectF(center!!.x - d, center!!.y - d, center!!.x + d, center!!.y + d))
            startAngle = 30f
            setSweepAngle(180f)
            setStyle(Paint.Style.STROKE)
            setAlpha((255 * 0.7f).toInt())
            setWidth(r * 0.02f)
        }

        arcs[2] = Arc().apply {
            setColor(Color.parseColor("#EF8683"))
            setOval(RectF(center!!.x - d, center!!.y - d, center!!.x + d, center!!.y + d))
            startAngle = 30f + 180f
            setSweepAngle(180f)
            setStyle(Paint.Style.STROKE)
            setAlpha((255 * 0.7f).toInt())
            setWidth(r * 0.02f)
        }

        d = base + 2 * r / 7
        arcs[3] = Arc().apply {
            setColor(Color.parseColor("#7CC0B1"))
            setOval(RectF(center!!.x - d, center!!.y - d, center!!.x + d, center!!.y + d))
            startAngle = 120f
            setSweepAngle(180f)
            setStyle(Paint.Style.STROKE)
            setAlpha((255 * 0.7f).toInt())
            setWidth(r * 0.02f)
        }

        arcs[4] = Arc().apply {
            setColor(Color.parseColor("#7CC0B1"))
            setOval(RectF(center!!.x - d, center!!.y - d, center!!.x + d, center!!.y + d))
            startAngle = 120f + 180f
            setSweepAngle(180f)
            setStyle(Paint.Style.STROKE)
            setAlpha((255 * 0.7f).toInt())
            setWidth(r * 0.02f)
        }

        d = base + 3 * r / 7
        arcs[5] = Arc().apply {
            setColor(Color.parseColor("#4690CE"))
            setOval(RectF(center!!.x - d, center!!.y - d, center!!.x + d, center!!.y + d))
            startAngle = 50f
            setSweepAngle(180f)
            setStyle(Paint.Style.STROKE)
            setAlpha((255 * 0.7f).toInt())
            setWidth(r * 0.02f)
        }
        arcs[6] = Arc().apply {
            setColor(Color.parseColor("#4690CE"))
            setOval(RectF(center!!.x - d, center!!.y - d, center!!.x + d, center!!.y + d))
            startAngle = 50f + 180f
            setSweepAngle(180f)
            setStyle(Paint.Style.STROKE)
            setAlpha((255 * 0.7f).toInt())
            setWidth(r * 0.02f)
        }
    }

    override fun setUpAnimation() {
        val fadeAnimators: Array<ValueAnimator?> = arrayOfNulls(4)
        fadeAnimators[0] = ValueAnimator.ofFloat(
                arcs[0]!!.startAngle,
                arcs[0]!!.startAngle + 360
            ).also {
                it.repeatCount = ValueAnimator.INFINITE
            it.duration = 1000
            it.addUpdateListener { ani ->
                rotates[0] = ani.animatedValue as Float
                invalidateListener?.let { listen-> listen.reDraw() }
            }
        }
        fadeAnimators[1] = ValueAnimator.ofFloat(
            0f,
            360f,
            0f
        ).also {
            it.repeatCount = ValueAnimator.INFINITE
            it.duration = 2000
            it.addUpdateListener { ani ->
                rotates[1] = ani.animatedValue as Float
                rotates[2] = ani.animatedValue as Float
                invalidateListener?.let { listen-> listen.reDraw() }
            }
        }
        fadeAnimators[2] = ValueAnimator.ofFloat(
            0f,
            360f,
            0f
        ).also {
            it.repeatCount = ValueAnimator.INFINITE
            it.duration = 3000
            it.addUpdateListener { ani ->
                rotates[3] = ani.animatedValue as Float
                rotates[4] = ani.animatedValue as Float
                invalidateListener?.let { listen-> listen.reDraw() }
            }
        }
        fadeAnimators[3] = ValueAnimator.ofFloat(
            0f,
            360f,
            0f
        ).also {
            it.repeatCount = ValueAnimator.INFINITE
            it.duration = 4000
            it.addUpdateListener { ani ->
                rotates[5] = ani.animatedValue as Float
                rotates[6] = ani.animatedValue as Float
                invalidateListener?.let { listen-> listen.reDraw() }
            }
        }

        fadeAnimators[0]?.start()
        fadeAnimators[1]?.start()
        fadeAnimators[2]?.start()
        fadeAnimators[3]?.start()
    }

    override fun draw(canvas: Canvas?) {
        for (i in 0 until numberOfArc) {
            canvas!!.save()
            canvas.rotate(rotates[i], center!!.x, center!!.y)
            arcs[i]!!.draw(canvas)
            canvas.restore()
        }
    }
}