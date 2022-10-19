package com.samin.carerobot.LoadingPage

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.coai.uikit.load.LoaderView
import com.samin.carerobot.MainActivity
import com.samin.carerobot.R

class LoadingDialog constructor(context: Context) : Dialog(context) {
    lateinit var loadingView: LoaderView
    lateinit var textView: TextView

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCanceledOnTouchOutside(false)
        setContentView(R.layout.loading_dialog_veiw)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}