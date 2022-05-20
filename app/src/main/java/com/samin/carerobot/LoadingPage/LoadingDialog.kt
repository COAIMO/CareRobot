package com.samin.carerobot.LoadingPage

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.coai.uikit.load.LoaderView

class LoadingDialog constructor(context: Context) : Dialog(context) {
    lateinit var loadingView: LoaderView
    lateinit var textView: TextView

    init {
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        loadingView = LoaderView(context)
        textView = TextView(context)
        textView.text = "LOADING"
        setCanceledOnTouchOutside(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setContentView(LoaderView(context))
    }
}