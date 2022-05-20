package com.samin.carerobot.LoadingPage

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF

abstract class ViewSupport {
    var color: Int = 0
    var width: Int = 0
    var height: Int = 0
    var desiredWidth: Int = 600
        protected set
    var desiredHeight: Int = 600
        protected set

    var context: Context? = null

    protected var center: PointF? = null
    protected var invalidateListener: InvalidateListener? = null

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        center = PointF(width / 2.0f, height / 2.0f)
    }

    @JvmName("setInvalidateListenerJvm")
    fun setInvalidateListener(invalidateListener: InvalidateListener?) {
        this.invalidateListener = invalidateListener
    }

    abstract fun initializeObjects()
    abstract fun setUpAnimation()
    abstract fun draw(canvas: Canvas?)

    val isDetached: Boolean
        get() = invalidateListener == null

    fun onDetach() {
        if (invalidateListener != null) {
            invalidateListener = null
        }
    }
}
