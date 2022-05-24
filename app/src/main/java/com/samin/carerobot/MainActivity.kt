package com.samin.carerobot

import android.content.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.coai.uikit.load.LoaderView
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.NuriPosSpeedAclCtrl
import com.jeongmin.nurimotortester.Nuri.ProtocolMode
import com.jeongmin.nurimotortester.NurirobotMC
import com.samin.carerobot.LoadingPage.LoadingDialog
import com.samin.carerobot.Logics.*
import com.samin.carerobot.Service.SerialService
import com.samin.carerobot.databinding.ActivityMainBinding
import java.util.*
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
    lateinit var motorControllerParser: MotorControllerParser

    companion object {
        val TAG = "태그"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        sharedPreference = SharedPreference(this)
        motorControllerParser = MotorControllerParser(sharedViewModel)
        controllerPad = ControllerPad(sharedViewModel)
        setFragment()
        checkLogin()
        moveRobot()
        observeMotorState()

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

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                SerialService.ACTION_USB_PERMISSION_GRANTED -> {
                    Toast.makeText(
                        context,
                        "시리얼 포트가 정상 연결되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    serialService!!.isFeedBack = true
                    serialService?.feedback()
                }
                SerialService.ACTION_USB_PERMISSION_NOT_GRANTED -> Toast.makeText(
                    context,
                    "USB Permission Not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setFilters() {
        val filter = IntentFilter()
        filter.addAction(SerialService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(SerialService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(broadcastReceiver, filter)
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
                ProtocolMode.FEEDPos.byte.toInt() -> {
//                    val tmpPosData = msg.obj as NuriPosSpeedAclCtrl
//                    sharedViewModel.encoderPOSMapt.put(
//                        tmpPosData.ID!!,
//                        tmpPosData.Pos!! / 4096 * 360 * 100
//                    )
//                    Log.d(TEST, "data : ${HexDump.dumpHexString(msg.obj as ByteArray)}")
                    motorControllerParser.parser(msg.obj as ByteArray)

                }

            }
        }
    }
    val TEST = "테스트"
    lateinit var observeMotorStateThread: Thread
    private fun observeMotorState() {
        observeMotorStateThread = Thread {
            try {
                while (true) {
                    for ((key, value) in sharedViewModel.motorInfo) {
                        when (key) {
                            CareRobotMC.Left_Shoulder_Encoder.byte -> {
                                Log.d(TEST, "Left_Shoulder_Encoder : ${value.position}")
                                if (value.max_Alert!! || value.min_Alert!!) {
                                    stopMotor(CareRobotMC.Left_Shoulder.byte)
//                                Log.d(TEST, "Left_Shoulder_Encoder : 멈춤")
                                } else {

                                }
                            }
                            CareRobotMC.Left_Elbow_Encoder.byte -> {
                                Log.d(TEST, "Left_Elbow_Encoder : ${value.position}")
                                if (value.max_Alert!! || value.min_Alert!!) {
                                    stopMotor(CareRobotMC.Left_Elbow.byte)
//                                    Log.d(TEST, "Left_Elbow : 멈춤")
                                } else {
                                }
                            }
                            CareRobotMC.Right_Shoulder_Encoder.byte -> {
                                Log.d(TEST, "Right_Shoulder_Encoder : ${value.position}")
                                if (value.max_Alert!! || value.min_Alert!!) {
                                    stopMotor(CareRobotMC.Right_Shoulder.byte)
                                } else {
                                }
                            }
                            CareRobotMC.Right_Elbow_Encoder.byte -> {
                                Log.d(TEST, "Right_Elbow_Encoder : ${value.position}")
                                if (value.max_Alert!! || value.min_Alert!!) {
                                    stopMotor(CareRobotMC.Right_Elbow.byte)
                                    Log.d(TEST, "Right_Elbow_Encoder : 멈춤")
                                } else {
                                }
                            }
                        }
                    }
                    Thread.sleep(20)
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        observeMotorStateThread.start()
    }

    var exLeft_Shoulder_Encoder_Data: Float? = null
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

    private var tts: TextToSpeech? = null

    private fun initTextToSpeech() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    return@TextToSpeech
                }
            } else {

            }
        }
    }

    private fun ttsSpeak(strTTS: String) {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            var handled = false
            if (ControllerPad.isGamePad(event)) {
                if (event.repeatCount == 0) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            sharedViewModel.controlPart.value = CareRobotMC.Shoulder.byte
                            controllerPad.isUsable = true
                        }
                        KeyEvent.KEYCODE_BUTTON_B -> {
                            sharedViewModel.controlPart.value = CareRobotMC.Elbow.byte
                            controllerPad.isUsable = true
                        }
                        KeyEvent.KEYCODE_BUTTON_X -> {
                            sharedViewModel.controlPart.value = CareRobotMC.Wheel.byte
                            controllerPad.isUsable = true
                        }
                        KeyEvent.KEYCODE_BUTTON_Y -> {
                            sharedViewModel.controlPart.value = CareRobotMC.Waist.byte
                            controllerPad.isUsable = true
                        }
                        KeyEvent.KEYCODE_BUTTON_R1 -> {
                            sharedViewModel.controlPart.value = CareRobotMC.Right_Shoulder.byte
                            controllerPad.isUsable = true
                        }
                        KeyEvent.KEYCODE_BUTTON_L1 -> {
                            sharedViewModel.controlPart.value = CareRobotMC.Left_Shoulder.byte
                            controllerPad.isUsable = true
                        }
                        KeyEvent.KEYCODE_BUTTON_R2 -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Right_Elbow.byte
                        }
                        KeyEvent.KEYCODE_BUTTON_L2 -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Left_Elbow.byte
                        }
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            var handled = false
            if (ControllerPad.isGamePad(event)) {
                if (event.repeatCount == 0) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            sharedViewModel.controlPart.value = null
                            controllerPad.isUsable = false
                        }
                        KeyEvent.KEYCODE_BUTTON_B -> {
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_X -> {
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_Y -> {
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_R1 -> {
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_L1 -> {
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
                            stopRobot()
                        }
                        KeyEvent.KEYCODE_BUTTON_R2 -> {
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_L2 -> {
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
                        }
                        else -> {
                            keyCode.takeIf { isFireKey(it) }?.run {
                                handled = true
                            }
                        }
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun isFireKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A

    private fun moveRobot() {

        sharedViewModel.left_Joystick.observe(this) {
            val tmpRPM = getRPMMath(it)
            moveWheelchair(tmpRPM)
        }
        sharedViewModel.right_Joystick.observe(this) {
            if (!controllerPad.isUsable) {
                stopRobot()
                return@observe
            }
            when (sharedViewModel.controlPart.value) {
                CareRobotMC.Waist.byte -> {
                    serialService?.isAnotherJob = true
                    val tmp = getDirectionRPM(it)
                    sendParser.ControlAcceleratedSpeed(
                        CareRobotMC.Waist.byte,
                        (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                        tmp.Left,
                        0.1f
                    )
                    sharedViewModel.controlDirection = tmp.LeftDirection
                    serialService?.sendData(sendParser.Data!!.clone())
                    serialService?.isAnotherJob = false
                }
                CareRobotMC.Right_Shoulder.byte -> {
                    serialService?.isAnotherJob = true
                    val tmp = getShoulderDirectionRPM(it)
                    sendParser.ControlAcceleratedSpeed(
                        CareRobotMC.Right_Shoulder.byte,
                        (if (tmp.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
                        tmp.Right,
                        0.1f
                    )
                    sharedViewModel.controlDirection = tmp.RightDirection
                    if (controllerPad.isUsable) {
                        serialService?.sendData(sendParser.Data!!.clone())
                    }
                    serialService?.isAnotherJob = false
                }
                CareRobotMC.Left_Shoulder.byte -> {
                    serialService?.isAnotherJob = true
                    val tmp = getShoulderDirectionRPM(it)
                    sendParser.ControlAcceleratedSpeed(
                        CareRobotMC.Left_Shoulder.byte,
                        (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                        tmp.Left,
                        0.1f
                    )
                    sharedViewModel.controlDirection = tmp.LeftDirection
                    if (controllerPad.isUsable) {
                        serialService?.sendData(sendParser.Data!!.clone())
                    }
                    serialService?.isAnotherJob = false
                }
                CareRobotMC.Right_Elbow.byte -> {
                    serialService?.isAnotherJob = true
                    val tmp = getElbowDirectionRPM(it)
                    sendParser.ControlAcceleratedSpeed(
                        CareRobotMC.Right_Elbow.byte,
                        (if (tmp.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
                        tmp.Right,
                        0.1f
                    )
                    sharedViewModel.controlDirection = tmp.RightDirection
                    if (controllerPad.isUsable) {
                        serialService?.sendData(sendParser.Data!!.clone())
                    }
                    serialService?.isAnotherJob = false
                }
                CareRobotMC.Left_Elbow.byte -> {
//                    if(sharedViewModel.right_Elbow_isUsable.value!!){
                        serialService?.isAnotherJob = true
                        val tmp = getElbowDirectionRPM(it)
                        sendParser.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Elbow.byte,
                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                            tmp.Left,
                            0.1f
                        )
                        sharedViewModel.controlDirection = tmp.LeftDirection
                        if (controllerPad.isUsable) {
                            serialService?.sendData(sendParser.Data!!.clone())
                        }
                        serialService?.isAnotherJob = false
//                    }
                }
                CareRobotMC.Shoulder.byte -> {

                }
                CareRobotMC.Elbow.byte -> {

                }
            }

        }

    }

    private val sendParser: NurirobotMC = NurirobotMC()

    private fun moveWheelchair(tmpRPMInfo: MotorRPMInfo) {
        val sedate = ByteArray(20)
        sendParser.ControlAcceleratedSpeed(
            CareRobotMC.Left_Wheel.byte,
            (if (tmpRPMInfo.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
            tmpRPMInfo.Left,
            calcConcentrationLeft(tmpRPMInfo.Left)
        )

        sendParser.Data!!.copyInto(sedate, 0, 0, sendParser.Data!!.size)
        sendParser.ControlAcceleratedSpeed(
            CareRobotMC.Right_Wheel.byte,
            (if (tmpRPMInfo.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
            tmpRPMInfo.Right,
            calcConcentrationRight(tmpRPMInfo.Right)
        )
        sendParser.Data!!.copyInto(sedate, 10, 0, sendParser.Data!!.size)
        serialService?.sendData(sedate)
    }


    val MaxForward: Float = 40f //1326.9645

    private fun getRPMMath(coordinate: JoystickCoordinate): MotorRPMInfo {
        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        val joy_x = coordinate.x
        val joy_y = coordinate.y * -1f
        val angle = Math.toDegrees(atan2(joy_y, joy_x).toDouble())
        val radian = angle * Math.PI / 180f
        val r = abs(round(sqrt(joy_x.pow(2) + joy_y.pow(2)) * 100) / 100f)
        Log.d(
            TAG,
            "x: $joy_x\tY: $joy_y\t anlge: $angle\t r: $r"
        )

        if (joy_x > 0 && joy_y > 0) {
            //우회전
            left = MaxForward * r * joy_x
            right = MaxForward * r * joy_x * 0.75f
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else if (joy_x > 0 && joy_y < 0) {
            //후진 우회전
            left = -1 * MaxForward * r * joy_x
            right = -1 * MaxForward * r * joy_x * 0.75f
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        } else if (joy_x < 0 && joy_y > 0) {
            //좌회전
            left = MaxForward * r * joy_x * 0.75f
            right = MaxForward * r * joy_x
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else if (joy_x < 0 && joy_y < 0) {
            //후진 좌회전
            left = -1 * MaxForward * r * joy_x * 0.75f
            right = -1 * MaxForward * r * joy_x
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        } else if (joy_x == 0f && joy_y > 0) {
            //전진
            left = MaxForward * r
            right = MaxForward * r
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else if (joy_x == 0f && joy_y < 0) {
            //후진
            left = MaxForward * r/2
            right = MaxForward * r/2
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        } else if (joy_y == 0f && joy_x > 0) {
            //제자리 우회전
            left = MaxForward * r/2
            right = MaxForward * r/2
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CW
        } else if (joy_y == 0f && joy_x < 0) {
            //제자리 좌회전
            left = MaxForward * r/2
            right = MaxForward * r/2
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CCW
        }

        Log.d(
            TAG,
            "left: $left\t right : $right"
        )

        ret.Left = abs(left)
        ret.Right = abs(right)
        Log.d(
            TAG,
            " ret.Left: ${ret.Left}\t ret.right : ${ret.Right}\t ret.LeftDirection :${ret.LeftDirection}\t ret.RightDirection:${ret.RightDirection}"
        )

        return ret
    }

    private fun getDirectionRPM(coordinate: JoystickCoordinate): MotorRPMInfo {
        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        val joy_x = coordinate.x
        val joy_y = coordinate.y * -1f
        val r = abs(round(sqrt(joy_x.pow(2) + joy_y.pow(2)) * 100) / 100f)

        left = 200 * r
        right = 200 * r

        if (joy_y > 0) {
            ret.Left = left
            ret.LeftDirection = Direction.CCW
        } else {
            ret.Left = left
            ret.LeftDirection = Direction.CW
        }

        return ret
    }

    private fun getElbowDirectionRPM(coordinate: JoystickCoordinate): MotorRPMInfo {
        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        val joy_x = coordinate.x
        val joy_y = coordinate.y * -1f
        val r = abs(round(sqrt(joy_x.pow(2) + joy_y.pow(2)) * 100) / 100f)

        left = 2 * r
        right = 2 * r

        if (joy_y > 0) {
            ret.Left = left
            ret.Right = right
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else {
            ret.Left = left
            ret.Right = right
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        }
        return ret
    }

    private fun getShoulderDirectionRPM(coordinate: JoystickCoordinate): MotorRPMInfo {
        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        val joy_x = coordinate.x
        val joy_y = coordinate.y * -1f
        val r = abs(round(sqrt(joy_x.pow(2) + joy_y.pow(2)) * 100) / 100f)

        left = 2 * r
        right = 2 * r

        if (joy_y > 0) {
            ret.Left = left
            ret.Right = right
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else {
            ret.Left = left
            ret.Right = right
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        }

//        sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte].

        return ret
    }

    private fun calcConcentrationLeft(curr: Float): Float {
        var ret: Float = 0.1f

        val calc = curr / 40f
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
        val calc = curr / 40f
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

    fun stopMotor(id_1: Byte, id_2: Byte? = null) {
        val nuriMC = NurirobotMC()
        serialService!!.isAnotherJob = true
        for (count in 0..2){
            if (id_2 == null) {
                nuriMC.ControlAcceleratedSpeed(id_1, Direction.CCW.direction, 0f, 0.1f)
                serialService?.sendData(nuriMC.Data!!.clone())
            } else {
                val sedate = ByteArray(22)
                nuriMC.ControlPosSpeed(id_1, Direction.CCW.direction, 0f, 0f)
                nuriMC.Data!!.clone().copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                nuriMC.ControlPosSpeed(id_2, Direction.CCW.direction, 0f, 0f)
                nuriMC.Data!!.clone().copyInto(sedate, 11, 0, nuriMC.Data!!.size)
                serialService?.sendData(sedate)
            }
            Thread.sleep(10)
        }
        serialService!!.isAnotherJob = false

    }

    private fun stopRobot() {
        val nuriMC = NurirobotMC()
        serialService?.isAnotherJob = true
        for (i in CareRobotMC.Left_Shoulder.byte..CareRobotMC.Left_Wheel.byte) {
            nuriMC.ControlAcceleratedSpeed(i.toByte(), Direction.CCW.direction, 0f, 0.1f)
            serialService?.sendData(nuriMC.Data!!.clone())
            Thread.sleep(20)
        }
        serialService!!.isAnotherJob = false
    }

    lateinit var setModeThread: Thread
    private fun setstandardmode() {
        setModeThread = Thread {


        }
        setModeThread.start()
    }

}