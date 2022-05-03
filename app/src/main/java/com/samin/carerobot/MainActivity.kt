package com.samin.carerobot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.samin.carerobot.Logics.ControllerPad
import com.samin.carerobot.Logics.SharedPreference
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.Service.SerialService
import com.samin.carerobot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var mBinding: ActivityMainBinding
    lateinit var loginFragment: LoginFragment
    lateinit var newAccountFragment: NewAccountFragment
    lateinit var mainFragment: MainFragment
    lateinit var sharedPreference: SharedPreference
    var serialService: SerialService? = null
    var isSerialSevice = false
    private val controllerPad = ControllerPad()

    companion object {
        val TAG = "태그"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setFragment()
        sharedPreference = SharedPreference(this)
        checkLogin()
    }

    override fun onResume() {
        super.onResume()
        bindSerialService()
    }

    private fun setFragment() {
        loginFragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.HostFragment_container, loginFragment).commit()
        newAccountFragment = NewAccountFragment()
        mainFragment = MainFragment()
    }

    fun onFragmentChange(index: Int) {
        when (index) {
            SharedViewModel.LOGINFRAGMENT -> supportFragmentManager.beginTransaction()
                .replace(R.id.HostFragment_container, loginFragment).commit()
            SharedViewModel.NEWACCOUNTFRAGMENT -> supportFragmentManager.beginTransaction()
                .replace(R.id.HostFragment_container, newAccountFragment).commit()
            SharedViewModel.MAINFRAGMENT -> supportFragmentManager.beginTransaction()
                .replace(R.id.HostFragment_container, mainFragment).commit()
        }
    }

    private fun checkLogin() {
        if (sharedPreference.checkloginState()) {
            onFragmentChange(SharedViewModel.MAINFRAGMENT)
        } else onFragmentChange(SharedViewModel.LOGINFRAGMENT)
    }

    fun bindSerialService() {
        val usbSerialServiceIntent = Intent(this, SerialService::class.java)
        bindService(usbSerialServiceIntent, serialServiceConnection, Context.BIND_AUTO_CREATE)
    }

    val serialServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SerialService.SerialServiceBinder
            serialService = binder.getService()
            //핸들러 연결
            serialService!!.setHandler(datahandler)
            isSerialSevice = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isSerialSevice = false
            Toast.makeText(this@MainActivity, "서비스 연결 해제", Toast.LENGTH_SHORT).show()
        }
    }

    val datahandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {

        event?.actionButton
        event?.source

//        if (ControllerPad.isDpadDevice(event!!)) {
//            when (controllerPad.getDirectionPressed(event)) {
//                ControllerPad.UP -> {
//                    // Do something for UP direction press
//                    Log.d(TAG, "UP")
//
//                    return true
//                }
//                ControllerPad.RIGHT -> {
//                    // Do something for RIGHT direction press
//                    Log.d(TAG, "RIGHT")
//
//                    return true
//                }
//                ControllerPad.DOWN -> {
//                    // Do something for UP direction press
//                    Log.d(TAG, "DOWN")
//
//                    return true
//                }
//                ControllerPad.LEFT -> {
//                    // Do something for LEFT direction press
//                    Log.d(TAG, "LEFT")
//
//                    return true
//                }
//                ControllerPad.CENTER -> {
//                    // Do something for LEFT direction press
//                    Log.d(TAG, "CENTER")
//
//                    return true
//                }
//            }
//        }
//        return true
        return if (ControllerPad.isJoyStick(event!!)) {
            // Process the movements starting from the
            // earliest historical position in the batch
            (0 until event.historySize).forEach { i ->
                // Process the event at historical position i
                controllerPad.processJoystickInput(event, i)
            }
            // Process the current movement sample in the batch (position -1)
            controllerPad.processJoystickInput(event, -1)
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            var handled = false
            if (ControllerPad.isGamePad(event)) {
                if (event.repeatCount == 0) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A ->
                            Log.d(TAG, "KEYCODE_BUTTON_A")
                        KeyEvent.KEYCODE_BUTTON_B ->
                            Log.d(TAG, "KEYCODE_BUTTON_B")
                        KeyEvent.KEYCODE_BUTTON_X ->
                            Log.d(TAG, "KEYCODE_BUTTON_X")
                        KeyEvent.KEYCODE_BUTTON_Y ->
                            Log.d(TAG, "KEYCODE_BUTTON_Y")
                        KeyEvent.KEYCODE_BUTTON_R1 ->
                            Log.d(TAG, "KEYCODE_BUTTON_R1")
                        KeyEvent.KEYCODE_BUTTON_L1 ->
                            Log.d(TAG, "KEYCODE_BUTTON_L1")
                        KeyEvent.KEYCODE_BUTTON_R2 ->
                            Log.d(TAG, "KEYCODE_BUTTON_R1")
                        KeyEvent.KEYCODE_BUTTON_L2 ->
                            Log.d(TAG, "KEYCODE_BUTTON_L1")
                        else -> {
                            keyCode.takeIf { isFireKey(it) }?.run {
                                handled = true
                            }
                        }
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isFireKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A
}