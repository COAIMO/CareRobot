package com.samin.carerobot.LoadingPage

import android.graphics.Canvas
import android.graphics.Paint

abstract class GraphicObject {
    protected var paint: Paint? = null
    init {
        paint = Paint()
        paint!!.isAntiAlias = true
    }

    open fun setColor(color: Int) {
        paint!!.color = color
    }

    open fun setAlpha(alpha: Int) {
        paint!!.alpha = alpha
    }

    open fun setWidth(width: Float) {
        paint!!.strokeWidth = width
    }

    open fun setStyle(style: Paint.Style?) {
        paint!!.style = style
    }

    abstract fun draw(canvas: Canvas?)
}