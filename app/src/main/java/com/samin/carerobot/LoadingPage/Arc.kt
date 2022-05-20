package com.samin.carerobot.LoadingPage

import android.graphics.Canvas
import android.graphics.RectF

class Arc : GraphicObject() {
    private val TAG = "Arc"
    private var oval: RectF? = null
    var startAngle = 0f
    private var sweepAngle = 0f
    private var useCenter = false
    private var count: Byte = 0
    fun setOval(oval: RectF?) {
        this.oval = oval
    }

    fun setSweepAngle(sweepAngle: Float) {
        this.sweepAngle = sweepAngle
    }

    fun setUseCenter(useCenter: Boolean) {
        this.useCenter = useCenter
    }

    override fun draw(canvas: Canvas?) {
        val angle: Float = sweepAngle * ( Math.abs(count.toInt()) / 128f)
//        canvas?.let { it.drawArc(oval!!, startAngle, sweepAngle, useCenter, paint!!) }
//        Log.d(TAG, "$angle $sweepAngle ======================================================")
        canvas?.let { it.drawArc(oval!!, startAngle, angle, useCenter, paint!!) }
        count = (count + 2).toByte()
    }
}