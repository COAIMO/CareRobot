package com.samin.carerobot

import android.app.Service
import android.content.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.jeongmin.nurimotortester.Nuri.Direction
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
        loadingView = LoadingDialog(this)


    }

    override fun onStart() {
        super.onStart()
        bindMessengerService()
    }

    override fun onResume() {
        super.onResume()
        setFilters()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindMessengerService()
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

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                SerialService.ACTION_USB_PERMISSION_GRANTED -> {
                    Toast.makeText(
                        context,
                        "시리얼 포트가 정상 연결되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private val serialSVCIPCHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SerialService.MSG_SERIAL_CONNECT -> {
                    isFeedBack = true
                    feedback()
                    Thread.sleep(1000)
                    observeMotorState()
                    Thread.sleep(1000)

                }
                ProtocolMode.FEEDPos.byte.toInt() -> {
                    motorControllerParser.parser(msg.obj as ByteArray)
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    private val serialSVCIPCClient = Messenger(serialSVCIPCHandler)
    private var serialSVCIPCService: Messenger? = null
    private val serialSVCIPCServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serialSVCIPCService = Messenger(service).apply {
                send(Message.obtain(null, SerialService.MSG_BIND_CLIENT, 0, 0).apply {
                    replyTo = serialSVCIPCClient
                })
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serialSVCIPCService = null
        }
    }

    private fun bindMessengerService() {
        Intent(this, SerialService::class.java).run {
            bindService(this, serialSVCIPCServiceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    private fun unbindMessengerService() {
        serialSVCIPCService?.send(
            Message.obtain(null, SerialService.MSG_UNBIND_CLIENT, 0, 0).apply {
                replyTo = serialSVCIPCClient
            })
        unbindService(serialSVCIPCServiceConnection)
    }

    fun sendProtocolToSerial(data: ByteArray) {
        val msg = Message.obtain(null, SerialService.MSG_SERIAL_SEND)
        val bundle = Bundle()
        bundle.putByteArray("", data)
        msg.data = bundle
        serialSVCIPCService?.send(msg)
    }

    fun checkEncorder(): Boolean {
        var ret = false
        ret = sharedViewModel.motorInfo.size >= 4
        return ret
    }

    val TEST = "테스트"
    lateinit var observeMotorStateThread: Thread
    val isCheckedEncorder = false
    private fun observeMotorState() {
        observeMotorStateThread = Thread {
            while (true) {
                try {
//                    if (!checkEncorder()) {
//                        Thread.sleep(1000)
//                    }
                    if (sharedViewModel.motorInfo.isNullOrEmpty()) {
                        Thread.sleep(1000)
                    } else {
                        for ((key, value) in sharedViewModel.motorInfo) {
                            when (key) {
                                CareRobotMC.Left_Shoulder_Encoder.byte -> {
                                    Log.d(
                                        TEST,
                                        "Left_Shoulder_Encoder : ${value.position} sensor: ${value.proximity_Sensor}"
                                    )
                                    if (value.max_Alert!! || value.min_Alert!!) {
                                        val msg = dataHandler.obtainMessage(
                                            SerialService.MSG_STOP_MOTOR,
                                            CareRobotMC.Left_Shoulder.byte
                                        )
                                        dataHandler.sendMessage(msg)
//                                        Log.d(TEST, "Left_Shoulder_Encoder : 멈춤")
                                    } else {
                                    }
                                }
                                CareRobotMC.Left_Elbow_Encoder.byte -> {
                                    Log.d(
                                        TEST,
                                        "Left_Elbow_Encoder : ${value.position} sensor: ${value.proximity_Sensor}"
                                    )
                                    if (value.max_Alert!! || value.min_Alert!!) {
                                        val msg = dataHandler.obtainMessage(
                                            SerialService.MSG_STOP_MOTOR,
                                            CareRobotMC.Left_Elbow.byte
                                        )
                                        dataHandler.sendMessage(msg)

//                                    Log.d(TEST, "Left_Elbow : 멈춤")
                                    } else {
                                    }
                                }
                                CareRobotMC.Right_Shoulder_Encoder.byte -> {
//                                Log.d(TEST, "Right_Shoulder_Encoder : ${value.position} sensor: ${value.proximity_Sensor}")
                                    if (value.max_Alert!! || value.min_Alert!!) {
                                        val msg = dataHandler.obtainMessage(
                                            SerialService.MSG_STOP_MOTOR,
                                            CareRobotMC.Right_Shoulder.byte
                                        )
                                        dataHandler.sendMessage(msg)
                                    } else {
                                    }
                                }
                                CareRobotMC.Right_Elbow_Encoder.byte -> {
//                                Log.d(TEST, "Right_Elbow_Encoder : ${value.position} sensor: ${value.proximity_Sensor}")
                                    if (value.max_Alert!! || value.min_Alert!!) {
                                        val msg = dataHandler.obtainMessage(
                                            SerialService.MSG_STOP_MOTOR,
                                            CareRobotMC.Right_Elbow.byte
                                        )
                                        dataHandler.sendMessage(msg)
                                        Log.d(TEST, "Right_Elbow_Encoder : 멈춤")
                                    } else {
                                    }
                                }
                                CareRobotMC.Sensor.byte -> {
                                    Log.d(
                                        TEST,
                                        "Sensor : ${value.position} sensor: ${value.proximity_Sensor}"
                                    )

                                }
                            }
                            if (sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]?.proximity_Sensor!! ||
                                sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]?.proximity_Sensor!!
                            ) {
                                if (sharedViewModel.controlDirection == Direction.CCW) {
                                    val msg = dataHandler.obtainMessage(
                                        SerialService.MSG_STOP_MOTOR,
                                        CareRobotMC.Waist.byte
                                    )
                                    dataHandler.sendMessage(msg)
                                }else{
                                }
                            }

                            if (sharedViewModel.motorInfo[CareRobotMC.Sensor.byte]?.proximity_Sensor!!) {
                                if (sharedViewModel.controlDirection == Direction.CW) {
                                    val msg = dataHandler.obtainMessage(
                                        SerialService.MSG_STOP_MOTOR,
                                        CareRobotMC.Waist.byte
                                    )
                                    dataHandler.sendMessage(msg)
                                }else{
                                }
                            }
                        }
                        Thread.sleep(20)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

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
            val nuriMC = NurirobotMC()
            if (ControllerPad.isGamePad(event)) {
                if (event.repeatCount == 0) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A -> {

                        }
                        KeyEvent.KEYCODE_BUTTON_B -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Elbow.byte
                        }
                        KeyEvent.KEYCODE_BUTTON_X -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Wheel.byte
                        }
                        KeyEvent.KEYCODE_BUTTON_Y -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Waist.byte
                        }
                        KeyEvent.KEYCODE_BUTTON_R1 -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Right_Shoulder.byte
                        }
                        KeyEvent.KEYCODE_BUTTON_L1 -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Left_Shoulder.byte
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
                            controllerPad.isUsable = false
                            sharedViewModel.controlPart.value = null
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
            if (!controllerPad.isUsable) {
                stopRobot()
                return@observe
            } else {
                val tmpRPM = getRPMMath(it)
                moveWheelchair(tmpRPM)
            }
        }
        sharedViewModel.right_Joystick.observe(this) {
            if (!controllerPad.isUsable) {
                stopRobot()
                return@observe
            } else {
                when (sharedViewModel.controlPart.value) {
                    CareRobotMC.Waist.byte -> {
                        isAnotherJob = true
                        val tmp = getDirectionRPM(it)
                        sendParser.ControlAcceleratedSpeed(
                            CareRobotMC.Waist.byte,
                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                            tmp.Left,
                            0.1f
                        )
                        sharedViewModel.controlDirection = tmp.LeftDirection
                        if (controllerPad.isUsable) {
                            sendProtocolToSerial(sendParser.Data!!.clone())
                        }
                        isAnotherJob = false
                    }
                    CareRobotMC.Right_Shoulder.byte -> {
                        isAnotherJob = true
                        val tmp = getShoulderDirectionRPM(it)
                        sendParser.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Shoulder.byte,
                            (if (tmp.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
                            tmp.Right,
                            0.1f
                        )
                        sharedViewModel.controlDirection = tmp.RightDirection
                        if (controllerPad.isUsable) {
                            sendProtocolToSerial(sendParser.Data!!.clone())
                        }
                        isAnotherJob = false
                    }
                    CareRobotMC.Left_Shoulder.byte -> {
                        isAnotherJob = true
                        val tmp = getShoulderDirectionRPM(it)
                        sendParser.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Shoulder.byte,
                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                            tmp.Left,
                            0.1f
                        )
                        sharedViewModel.controlDirection = tmp.LeftDirection
                        if (controllerPad.isUsable) {
                            sendProtocolToSerial(sendParser.Data!!.clone())
                        }
                        isAnotherJob = false
                    }
                    CareRobotMC.Right_Elbow.byte -> {
                        isAnotherJob = true
                        val tmp = getElbowDirectionRPM(it)
                        sendParser.ControlAcceleratedSpeed(
                            CareRobotMC.Right_Elbow.byte,
                            (if (tmp.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
                            tmp.Right,
                            0.1f
                        )
                        sharedViewModel.controlDirection = tmp.RightDirection
                        if (controllerPad.isUsable) {
                            sendProtocolToSerial(sendParser.Data!!.clone())
                        }
                        isAnotherJob = false
                    }
                    CareRobotMC.Left_Elbow.byte -> {
                        isAnotherJob = true
                        val tmp = getElbowDirectionRPM(it)
                        sendParser.ControlAcceleratedSpeed(
                            CareRobotMC.Left_Elbow.byte,
                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                            tmp.Left,
                            0.1f
                        )
                        sharedViewModel.controlDirection = tmp.LeftDirection
                        if (controllerPad.isUsable) {
                            sendProtocolToSerial(sendParser.Data!!.clone())
                        }
                        isAnotherJob = false
                    }

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
        sendProtocolToSerial(sedate)
    }


    val MaxForward: Float = 40f / 10   //1326.9645

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
            left = MaxForward * r / 2
            right = MaxForward * r / 2
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        } else if (joy_y == 0f && joy_x > 0) {
            //제자리 우회전
            left = MaxForward * r / 2
            right = MaxForward * r / 2
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CW
        } else if (joy_y == 0f && joy_x < 0) {
            //제자리 좌회전
            left = MaxForward * r / 2
            right = MaxForward * r / 2
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
        for (count in 0..2) {
            if (id_2 == null) {
                nuriMC.ControlAcceleratedSpeed(id_1, Direction.CCW.direction, 0f, 0.1f)
                sendProtocolToSerial(nuriMC.Data!!.clone())
            } else {
                val sedate = ByteArray(22)
                nuriMC.ControlPosSpeed(id_1, Direction.CCW.direction, 0f, 0f)
                nuriMC.Data!!.clone().copyInto(sedate, 0, 0, nuriMC.Data!!.size)
                nuriMC.ControlPosSpeed(id_2, Direction.CCW.direction, 0f, 0f)
                nuriMC.Data!!.clone().copyInto(sedate, 11, 0, nuriMC.Data!!.size)
                sendProtocolToSerial(sedate)

            }
        }
    }

    private fun stopRobot() {
        val nuriMC = NurirobotMC()
        for (i in CareRobotMC.Left_Shoulder.byte..CareRobotMC.Left_Wheel.byte) {
            nuriMC.ControlAcceleratedSpeed(i.toByte(), Direction.CCW.direction, 0f, 0.1f)
            sendProtocolToSerial(nuriMC.Data!!.clone())
            Thread.sleep(20)
        }
    }


    private lateinit var changeRobotPositionThread: Thread
    private lateinit var loadingView: LoadingDialog

    fun robotModeChange(mode: Int, setView: Int) {
        loadingView.show()
        val nuriMC = NurirobotMC()
        var step_1 = true
        var step_2 = true
        var right_ShoulderSet = false
        var left_ShoulderSet = false
        var right_ElbowSet = false
        var left_ElbowSet = false

        when (mode) {
            1 -> {
                changeRobotPositionThread = Thread {
                    try {
                        while (step_1) {
                            val rightShoulder_tmp =
                                sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
                            val leftShoulder_tmp =
                                sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
                            if (rightShoulder_tmp?.position!! > 2 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 10) {
                                right_ShoulderSet = false
                                if (rightShoulder_tmp?.position!! < 4) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Shoulder.byte,
                                        Direction.CW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Shoulder.byte,
                                        Direction.CW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (rightShoulder_tmp.min_Range!! < rightShoulder_tmp.position!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!! - 10) {
                                right_ShoulderSet = false
                                if (rightShoulder_tmp.position!! > 362) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Shoulder.byte,
                                        Direction.CCW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Shoulder.byte,
                                        Direction.CCW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (rightShoulder_tmp.position!! in 0.0..2.0) {
                                if (!right_ShoulderSet) {
                                    val msg = dataHandler.obtainMessage(
                                        SerialService.MSG_STOP_MOTOR,
                                        CareRobotMC.Right_Shoulder.byte
                                    )
                                    dataHandler.sendMessage(msg)
                                }
                                right_ShoulderSet = true
                            }

                            if (leftShoulder_tmp?.position!! > 2 && leftShoulder_tmp.position!! <= leftShoulder_tmp.min_Range!! + 10) {
                                left_ShoulderSet = false
                                if (leftShoulder_tmp.position!! < 4) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Shoulder.byte,
                                        Direction.CW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Shoulder.byte,
                                        Direction.CW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (leftShoulder_tmp.position!! > leftShoulder_tmp.min_Range!! && leftShoulder_tmp.position!! > leftShoulder_tmp.max_Range!! - 10) {
                                left_ShoulderSet = false
                                if (leftShoulder_tmp.position!! > 362) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Shoulder.byte,
                                        Direction.CCW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Shoulder.byte,
                                        Direction.CCW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (leftShoulder_tmp.position!! in 0.0..2.0) {
                                if (!left_ShoulderSet) {
                                    val msg = dataHandler.obtainMessage(
                                        SerialService.MSG_STOP_MOTOR,
                                        CareRobotMC.Left_Shoulder.byte
                                    )
                                    dataHandler.sendMessage(msg)
                                }
                                left_ShoulderSet = true
                            }

                            if (right_ShoulderSet && left_ShoulderSet) {
                                step_1 = false
                            }
                        }

                        while (step_2) {
                            val rightElbow_tmp =
                                sharedViewModel.motorInfo[CareRobotMC.Right_Elbow_Encoder.byte]
                            val leftElbow_tmp =
                                sharedViewModel.motorInfo[CareRobotMC.Left_Elbow_Encoder.byte]

                            if (rightElbow_tmp?.position!! > rightElbow_tmp.min_Range!! - 10 && rightElbow_tmp.position!! < 94) {
                                right_ElbowSet = false
                                if (rightElbow_tmp.position!! > 92) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Elbow.byte,
                                        Direction.CCW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Elbow.byte,
                                        Direction.CCW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (96 < rightElbow_tmp.position!! && rightElbow_tmp.position!! < rightElbow_tmp.max_Range!! + 10) {
                                right_ElbowSet = false
                                if (rightElbow_tmp.position!! < 99) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Elbow.byte,
                                        Direction.CW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Right_Elbow.byte,
                                        Direction.CW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (rightElbow_tmp.position!! in 94.0..96.0) {
                                if (!right_ElbowSet) {
                                    stopMotor(CareRobotMC.Right_Elbow.byte)
                                }
                                right_ElbowSet = true
                            }

                            if (leftElbow_tmp?.position!! > leftElbow_tmp.min_Range!! - 10 && leftElbow_tmp.position!! < 264) {
                                left_ElbowSet = false
                                if (leftElbow_tmp.position!! > 262) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Elbow.byte,
                                        Direction.CCW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Elbow.byte,
                                        Direction.CCW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (266 < leftElbow_tmp.position!! && leftElbow_tmp.position!! < leftElbow_tmp.max_Range!! + 10) {
                                left_ElbowSet = false
                                if (leftElbow_tmp.position!! < 268) {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Elbow.byte,
                                        Direction.CW.direction,
                                        0.3f,
                                        0.1f
                                    )
                                } else {
                                    nuriMC.ControlAcceleratedSpeed(
                                        CareRobotMC.Left_Elbow.byte,
                                        Direction.CW.direction,
                                        1f,
                                        0.1f
                                    )
                                }
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(20)
                            } else if (leftElbow_tmp.position!! in 264.0..266.0) {
                                if (!left_ElbowSet) {
                                    val msg = dataHandler.obtainMessage(
                                        SerialService.MSG_STOP_MOTOR,
                                        CareRobotMC.Left_Elbow.byte
                                    )
                                    dataHandler.sendMessage(msg)
                                }
                                left_ElbowSet = true
                            }

                            if (right_ElbowSet && left_ElbowSet) {
                                step_2 = false
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    runOnUiThread {
                        sharedViewModel.viewState.value = setView
                        loadingView.dismiss()
                    }
                }
                changeRobotPositionThread.start()
            }

            2 -> {
                changeRobotPositionThread = Thread {
                    while (step_1) {
                        val rightShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
                        val leftShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
                        if (rightShoulder_tmp?.position!! > 181 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 10) {
                            right_ShoulderSet = false
                            if (rightShoulder_tmp.position!! < 184) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (rightShoulder_tmp.position!! > rightShoulder_tmp.min_Range!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!! - 10) {
                            right_ShoulderSet = false
                            nuriMC.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Shoulder.byte,
                                Direction.CCW.direction,
                                1f,
                                0.1f
                            )
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (rightShoulder_tmp.position!! < 179) {
                            right_ShoulderSet = false
                            if (rightShoulder_tmp.position!! > 176) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (rightShoulder_tmp.position!! in 179.0..181.0) {
                            if (right_ShoulderSet) {
                                val msg = dataHandler.obtainMessage(
                                    SerialService.MSG_STOP_MOTOR,
                                    CareRobotMC.Right_Shoulder.byte
                                )
                                dataHandler.sendMessage(msg)
                            }
                            right_ShoulderSet = true
                        }

                        if (leftShoulder_tmp?.position!! < 179 && leftShoulder_tmp.position!! >= leftShoulder_tmp.max_Range!! - 10) {
                            left_ShoulderSet = false
                            if (leftShoulder_tmp.position!! > 176) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )

                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (leftShoulder_tmp.position!! < leftShoulder_tmp.min_Range!! + 10) {
                            left_ShoulderSet = false
                            nuriMC.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Shoulder.byte,
                                Direction.CW.direction,
                                2f,
                                0.1f
                            )
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (leftShoulder_tmp.position!! > 181) {
                            left_ShoulderSet = false
                            if (leftShoulder_tmp.position!! < 183) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (leftShoulder_tmp.position!! in 179.0..181.0) {
                            if (!left_ShoulderSet) {
                                val msg = dataHandler.obtainMessage(
                                    SerialService.MSG_STOP_MOTOR,
                                    CareRobotMC.Left_Shoulder.byte
                                )
                                dataHandler.sendMessage(msg)
                            }
                            left_ShoulderSet = true
                        }

                        if (right_ShoulderSet && left_ShoulderSet) {
                            step_1 = false
                        }
                    }

                    while (step_2) {
                        val rightElbow_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Right_Elbow_Encoder.byte]
                        val leftElbow_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Left_Elbow_Encoder.byte]

                        if (rightElbow_tmp?.position!! > rightElbow_tmp.min_Range!! - 10 && rightElbow_tmp.position!! < 94) {
                            right_ElbowSet = false
                            if (rightElbow_tmp.position!! > 92) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (96 < rightElbow_tmp.position!! && rightElbow_tmp.position!! < rightElbow_tmp.max_Range!! + 10) {
                            right_ElbowSet = false
                            if (rightElbow_tmp.position!! < 99) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Elbow.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (rightElbow_tmp.position!! in 94.0..96.0) {
                            if (!right_ElbowSet) {
                                stopMotor(CareRobotMC.Right_Elbow.byte)
                            }
                            right_ElbowSet = true
                        }

                        if (leftElbow_tmp?.position!! > leftElbow_tmp.min_Range!! - 10 && leftElbow_tmp.position!! < 264) {
                            left_ElbowSet = false
                            if (leftElbow_tmp.position!! > 262) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CCW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CCW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (266 < leftElbow_tmp.position!! && leftElbow_tmp.position!! < leftElbow_tmp.max_Range!! + 10) {
                            left_ElbowSet = false
                            if (leftElbow_tmp.position!! < 268) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CW.direction,
                                    0.3f,
                                    0.1f
                                )
                            } else {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Elbow.byte,
                                    Direction.CW.direction,
                                    1f,
                                    0.1f
                                )
                            }
                            val data = nuriMC.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Thread.sleep(20)
                        } else if (leftElbow_tmp.position!! in 264.0..266.0) {
                            if (!left_ElbowSet) {
                                val msg = dataHandler.obtainMessage(
                                    SerialService.MSG_STOP_MOTOR,
                                    CareRobotMC.Left_Elbow.byte
                                )
                                dataHandler.sendMessage(msg)
                            }
                            left_ElbowSet = true
                        }

                        if (right_ElbowSet && left_ElbowSet) {
                            step_2 = false
                        }
                    }

                    runOnUiThread {
                        sharedViewModel.viewState.value = setView
                        loadingView.dismiss()
                    }
                }
                changeRobotPositionThread.start()
            }
        }

    }

    lateinit var observeWaistThread: Thread
    var isInitePosition = true
    var isSetPosition = true

    var count = 0
    private fun initWaist() {
        loadingView.show()
        val nuriMC = NurirobotMC()
        sharedViewModel.controlDirection = Direction.CCW
        nuriMC.ControlAcceleratedSpeed(
            CareRobotMC.Waist.byte,
            Direction.CCW.direction,
            100f,
            0.1f
        )
        sendProtocolToSerial(nuriMC.Data!!.clone())

        observeWaistThread = Thread {
            val sendParser = NurirobotMC()
            while (true) {
                if (sharedViewModel.motorInfo.isNullOrEmpty()) {
                    Thread.sleep(100)

                } else {
                    while (isInitePosition) {
                        if (sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]?.proximity_Sensor!! ||
                            sharedViewModel.motorInfo[CareRobotMC.Left_Elbow_Encoder.byte]?.proximity_Sensor!!
                        ) {
                            if (sharedViewModel.controlDirection == Direction.CCW) {
                                stopMotor(CareRobotMC.Waist.byte)
                                Thread.sleep(500)
                                isAnotherJob = true
                                for (i in 0..3) {
                                    sendParser.ResetPostion(CareRobotMC.Waist.byte)
                                    val data = sendParser.Data!!.clone()
                                    val msg =
                                        dataHandler.obtainMessage(
                                            SerialService.MSG_SERIAL_SEND,
                                            data
                                        )
                                    dataHandler.sendMessage(msg)
                                    Thread.sleep(10)
                                }
                                isInitePosition = false
                                isAnotherJob = false

                            }
                        }
                    }

                    var pos: Float = 0f
                    while (isSetPosition) {

                        synchronized(sharedViewModel.lockobj) {
                            pos =
                                sharedViewModel.motorInfo[CareRobotMC.Waist.byte]!!.position ?: -1f
                        }

                        if (pos == 0f) {
                            isAnotherJob = true
                            sendParser.ControlAcceleratedPos(
                                CareRobotMC.Waist.byte,
                                Direction.CW.direction,
                                360f,
                                0.4f
                            )
                            val data = sendParser.Data!!.clone()
                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Log.d("허리", "msg_send : ${HexDump.toHexString(data)}\t")

                            Thread.sleep(20)
                            count++
                            Log.d("허리", "count : ${count}\t")
                            isAnotherJob = false

                        }
                        Thread.sleep(500)
                    }

                    runOnUiThread {
                        loadingView.dismiss()
                    }
                    Thread.sleep(20)
                }

            }

        }
        observeWaistThread.start()
    }

    var feedBackMotorStateInfoThread: Thread? = null
    var isFeedBack = false
    var isAnotherJob = false
    var isUsedWaist = true

    fun feedback() {
        feedBackMotorStateInfoThread = Thread {
            val sendParser = NurirobotMC()
            while (isFeedBack) {
                try {
                    while (isAnotherJob) {
                        Thread.sleep(10)
                    }
                    for (encorderID in CareRobotMC.Left_Shoulder_Encoder.byte..CareRobotMC.Sensor.byte) {
                        sendParser.Feedback(encorderID.toByte(), ProtocolMode.REQPos.byte)
                        val data = sendParser.Data!!.clone()
                        val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                        dataHandler.sendMessage(msg)
                        Thread.sleep(20)
                    }

//                    if (isUsedWaist) {
//                        sendParser.Feedback(CareRobotMC.Waist.byte, ProtocolMode.REQPos.byte)
//                        val data = sendParser.Data!!.clone()
//                        val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                        dataHandler.sendMessage(msg)
//                        Thread.sleep(20)
//                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        feedBackMotorStateInfoThread?.start()
    }

    val dataHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SerialService.MSG_SERIAL_SEND -> {
                    sendProtocolToSerial(msg.obj as ByteArray)
                    Log.d("허리", "handler_send : ${HexDump.toHexString(msg.obj as ByteArray)}\t")

                }
                SerialService.MSG_STOP_MOTOR -> {
                    stopMotor(msg.obj as Byte)
                }
                SerialService.MSG_ERROR -> {
                    Toast.makeText(
                        this@MainActivity,
                        "시리얼 포트가 정상 연결되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    inner class test : Thread() {
        val hh = true
        var i = 0
        override fun run() {
            Log.d("tt", "start")
            while (hh) {
                i++
                sleep(20)
                Log.d("tt", "$i")
                if (i > 100) {
                    interrupt()
                }
            }

        }

        override fun interrupt() {
            Log.d("tt", "interrupt")
            this.interrupt()
        }

        override fun destroy() {
            Log.d("tt", "destroy")
            this.destroy()
        }
    }
}