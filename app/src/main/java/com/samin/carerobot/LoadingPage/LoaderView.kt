package com.coai.uikit.load

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import com.samin.carerobot.LoadingPage.InvalidateListener
import com.samin.carerobot.LoadingPage.ViewSupport

class LoaderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), InvalidateListener {
    lateinit var loaderView : ViewSupport
    private val TAG = "LoaderView"

    init {
        initialize()
    }
    private fun initialize(){
//        Log.d(TAG, "initialize ======================================================")
        loaderView = Loader()
        loaderView.color = Color.parseColor("#ff000000")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        Log.d(TAG, "onMeasure ======================================================")
        val measuredWidth = resolveSize(loaderView.desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(loaderView.desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
//        Log.d(TAG, "onLayout ======================================================")
        super.onLayout(changed, left, top, right, bottom)
        loaderView.setSize(width, height)
        loaderView.initializeObjects()
        loaderView.setUpAnimation()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            loaderView.draw(it)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        loaderView.let {
            if (it.isDetached)
                it.setInvalidateListener(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loaderView.let { it.onDetach() }
    }

    override fun reDraw() {
        invalidate()
    }


}