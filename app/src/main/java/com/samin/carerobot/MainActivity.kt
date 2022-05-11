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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.ProtocolMode
import com.jeongmin.nurimotortester.NurirobotMC
import com.samin.carerobot.Logics.*
import com.samin.carerobot.Service.SerialService
import com.samin.carerobot.databinding.ActivityMainBinding
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    lateinit var mBinding: ActivityMainBinding
    lateinit var loginFragment: LoginFragment
    lateinit var newAccountFragment: NewAccountFragment
    lateinit var mainFragment: MainFragment
    lateinit var sharedPreference: SharedPreference
    var serialService: SerialService? = null
    var isSerialSevice = false
    lateinit var controllerPad: ControllerPad
    private lateinit var sharedViewModel: SharedViewModel

    companion object {
        val TAG = "태그"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        setFragment()
        sharedPreference = SharedPreference(this)
        checkLogin()
        controllerPad = ControllerPad(sharedViewModel)
        moveRobot()
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
            when (msg.what) {

            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
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
        val nuriMC = NurirobotMC()
        if (event != null) {
            var handled = false
            if (ControllerPad.isGamePad(event)) {
                if (event.repeatCount == 0) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            nuriMC.Feedback(0.toByte(), ProtocolMode.REQPos.byte)
                            serialService?.sendData(nuriMC.Data!!)
                        }
                        KeyEvent.KEYCODE_BUTTON_B -> {
                            nuriMC.Feedback(0.toByte(), ProtocolMode.REQSpeed.byte)
                            serialService?.sendData(nuriMC.Data!!)
                        }
                        KeyEvent.KEYCODE_BUTTON_X -> {
                            serialService?.feedBackPing()
                        }
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

    private fun moveRobot() {

//        val observer = Observer{
//        }
        sharedViewModel.left_Joystick_x.observe(this, {

        })

//        val gasunitObserver = Observer<Int> {
//            binding.viwLoader.setGasUnit(it)
//            binding.viwLoader1.setGasUnit(it)
//            binding.viwLoader2.setGasUnit(it)
//            binding.viwLoader3.setGasUnit(it)
//            binding.viwLoader4.setGasUnit(it)
//            binding.viwLoader5.setGasUnit(it)
//            binding.viwLoader6.setGasUnit(it)
//            binding.viwLoader7.setGasUnit(it)
//            binding.viwLoader8.setGasUnit(it)
//            binding.viwLoader9.setGasUnit(it)
//            binding.viwLoader10.setGasUnit(it)
//        }
//        model.gasUnit.observe(this, gasunitObserver)

        sharedViewModel.left_Joystick.observe(this) {
            val tmpRPM = getRPMMath(it)
            moveWheelchair(tmpRPM)
        }
        sharedViewModel.right_Joystick.observe(this){
        }

    }

    private val sendParser: NurirobotMC = NurirobotMC()

    private fun moveWheelchair(tmpRPMInfo: MotorRPMInfo) {
//        if (isLowBat) {
//            val sedate = ByteArray(20)
//            sendParser.ControlAcceleratedSpeed(
//                0,
//                (0x00).toByte(),
//                0F,
//                0.1F
//            )
//            sendParser.Data!!.copyInto(sedate, 0, 0, sendParser.Data!!.size)
//            sendParser.ControlAcceleratedSpeed(
//                1,
//                ( 0x00).toByte(),
//                0F,
//                0.1F
//            )
//            sendParser.Data!!.copyInto(sedate, 10, 0, sendParser.Data!!.size)
//            usbService?.write(sedate)
//            return
//        }

//        if (System.currentTimeMillis() - lastSendTime > 25) {
//            lastSendTime = System.currentTimeMillis()
        val sedate = ByteArray(20)
        sendParser.ControlAcceleratedSpeed(
            0,
            (if (tmpRPMInfo.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
            tmpRPMInfo.Left,
            calcConcentrationLeft(tmpRPMInfo.Left)
        )

        sendParser.Data!!.copyInto(sedate, 0, 0, sendParser.Data!!.size)
        sendParser.ControlAcceleratedSpeed(
            1,
            (if (tmpRPMInfo.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
            tmpRPMInfo.Right,
            calcConcentrationRight(tmpRPMInfo.Right)
        )
        sendParser.Data!!.copyInto(sedate, 10, 0, sendParser.Data!!.size)
        serialService?.sendData(sedate)
//        }
    }


    val MaxForward: Float = 40f //1326.9645
    val MaxBackward: Float = 885f // 884.6426044
    val MaxLeftRight: Float = 400f
    val turn_damping: Float = 2.2f

    private fun getRPMMath(coordinate: JoystickCoordinate): MotorRPMInfo {

        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        var joy_x = coordinate.x
        var joy_y = coordinate.y * -1f
        val angle = Math.toDegrees(atan2(joy_y, joy_x).toDouble())
        val r = sqrt(joy_x.pow(2) + joy_y.pow(2))

        var max_r = abs(r / joy_y)

        if (abs(joy_x) > abs(joy_y))
            max_r = abs(r / joy_x)

        val magnitude = r / max_r
        Log.d(
            TAG,
            "x: $joy_x\tY: $joy_y\t anlge: $angle\t r: $r"
        )
        left = magnitude * (sin(angle) + cos(angle)).toFloat()
        right = magnitude * (sin(angle) - cos(angle)).toFloat() * -1
        Log.d(
            TAG,
            "left: $left\tright: $right\t"
        )
        if (left.isNaN())
            left = 0f

        if (right.isNaN())
            right = 0f

        ret.Left = min(abs(left) * MaxForward, MaxForward)
        ret.Right = min(abs(right) * MaxForward, MaxForward)
        Log.d(
            TAG,
            "ret.Left: ${ret.Left}\tret.Right: ${ret.Right}\t"
        )
        ret.LeftDirection = Direction.CW
        ret.RightDirection = Direction.CW
        if (left >= 0f)
            ret.LeftDirection = Direction.CCW

        if (right >= 0f)
            ret.RightDirection = Direction.CCW

        Log.d(
            TAG,
            "x: $joy_x\tY: $joy_y\tleft:$left right: $right LeftDirection : ${ret.LeftDirection}\tRightDirection: ${ret.RightDirection}"
        )
        //


        return ret
    }

    private fun calcConcentrationLeft(curr: Float): Float {
        var ret: Float = 0.1f

//        if (rpmLeft < 400f) {
//            val calc = curr / 800f
//            if (calc < 0.2f)
//                ret = 0.1f
//            else if (calc < 0.4f)
//                ret = 0.2f
//            else if (calc < 0.6f)
//                ret = 0.3f
//            else if (calc < 0.8f)
//                ret = 0.5f
//            else
//                ret = 0.7f
//        } else {
//            ret = 0.1f
//        }
        val calc = curr / 800f
        if (calc < 0.2f)
            ret = 0.1f
        else if (calc < 0.4f)
            ret = 0.2f
        else if (calc < 0.6f)
            ret = 0.3f
        else if (calc < 0.8f)
            ret = 0.5f
        else
            ret = 0.7f
        return ret
    }

    private fun calcConcentrationRight(curr: Float): Float {
        var ret: Float = 0.1f

//        if (rpmRight < 400f) {
//            val calc = curr / 800f
//            if (calc < 0.2f)
//                ret = 0.1f
//            else if (calc < 0.4f)
//                ret = 0.2f
//            else if (calc < 0.6f)
//                ret = 0.3f
//            else if (calc < 0.8f)
//                ret = 0.5f
//            else
//                ret = 0.7f
//        } else {
//            ret = 0.1f
//        }
        val calc = curr / 800f
        if (calc < 0.2f)
            ret = 0.1f
        else if (calc < 0.4f)
            ret = 0.2f
        else if (calc < 0.6f)
            ret = 0.3f
        else if (calc < 0.8f)
            ret = 0.5f
        else
            ret = 0.7f
        return ret
    }
}