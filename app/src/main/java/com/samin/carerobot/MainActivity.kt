package com.samin.carerobot

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.InputDevice
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
import kotlinx.coroutines.*
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
    var waistIsUsable = false
    var leftShoulderIsUsable = false
    var rightShoulderIsUsable = false


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
        getGameControllerIds()

    }

    fun getGameControllerIds(): List<Int> {
        val gameControllerDeviceIds = mutableListOf<Int>()
        val deviceIds = InputDevice.getDeviceIds()
        deviceIds.forEach { deviceId ->
            InputDevice.getDevice(deviceId).apply {

                // Verify that the device has gamepad buttons, control sticks, or both.
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
                    || sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
                ) {
                    // This device is a game controller. Store its device ID.
                    gameControllerDeviceIds
                        .takeIf { !it.contains(deviceId) }
                        ?.add(deviceId)
                }
            }
        }
        return gameControllerDeviceIds
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
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    for (i in 0..2) {
                        stopRobot()
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                    for (i in 0..2) {
                        stopRobot()
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.d(
                        "블루투스",
                        "ACTION_ACL_CONNECTED"
                    )
                }
            }
        }
    }

    private fun setFilters() {
        val filter = IntentFilter()
        filter.addAction(SerialService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(SerialService.ACTION_USB_PERMISSION_NOT_GRANTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        registerReceiver(broadcastReceiver, filter)
    }

    private val serialSVCIPCHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SerialService.MSG_SERIAL_CONNECT -> {
//                    loadingView.show()
                    isFeedBack = true
                    feedback()
                    Thread.sleep(1000)
                    observeMotorState()
                    Thread.sleep(1000)
//                    initWaistPosition()
//                    initsetLegPosition()

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
                                CareRobotMC.Right_Shoulder_Encoder.byte -> {
                                    Log.d(
                                        TEST,
                                        "Right_Shoulder_Encoder : ${value.position} sensor: ${value.proximity_Sensor}"
                                    )
                                    if (value.max_Alert!! || value.min_Alert!!) {
                                        val msg = dataHandler.obtainMessage(
                                            SerialService.MSG_STOP_MOTOR,
                                            CareRobotMC.Right_Shoulder.byte
                                        )
                                        dataHandler.sendMessage(msg)
                                    } else {
                                    }
                                }
                                CareRobotMC.Waist_Sensor.byte -> {
//                                    value.sensorData == 1.toByte() || value.sensorData == 2.toByte() || value.sensorData == 3.toByte()
                                    // 센서 위에꺼만 들어올때
                                    Log.d("허리센서 ", "value:${value.sensorData}")
                                    Log.d("방향` ", "value:${sharedViewModel.controlDirection}")
                                    if (value.sensorData == 1.toByte()) {
                                        if (sharedViewModel.controlDirection == Direction.CW) {
                                            waistIsUsable = false
                                            val msg = dataHandler.obtainMessage(
                                                SerialService.MSG_STOP_MOTOR,
                                                CareRobotMC.Waist.byte
                                            )
                                            dataHandler.sendMessage(msg)
                                        } else {
                                            waistIsUsable = true
                                        }

//                                        value.sensorData == 4.toByte() || value.sensorData == 8.toByte() || value.sensorData == 12.toByte()
                                        // 센서 아래꺼만 들어왔을때
                                    } else if (value.sensorData == 4.toByte()) {
                                        if (sharedViewModel.controlDirection == Direction.CCW) {
                                            waistIsUsable = false
                                            val msg = dataHandler.obtainMessage(
                                                SerialService.MSG_STOP_MOTOR,
                                                CareRobotMC.Waist.byte
                                            )
                                            dataHandler.sendMessage(msg)
                                        } else {
                                            waistIsUsable = true
                                        }
                                        //센서 위아래 동시에 들어올때
                                    } else if (value.sensorData == 5.toByte()) {
                                        waistIsUsable = false
                                        val msg = dataHandler.obtainMessage(
                                            SerialService.MSG_STOP_MOTOR,
                                            CareRobotMC.Waist.byte
                                        )
                                        dataHandler.sendMessage(msg)
                                    } else {
                                        waistIsUsable = true
                                    }

                                }

                                CareRobotMC.Leg_Angle.byte -> {
                                    Log.d("다리", "위치 :${value.position}")
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

    var expression = 0

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            var handled = false
            val nuriMC = NurirobotMC()
            if (ControllerPad.isGamePad(event)) {
                if (event.repeatCount == 0) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            controllerPad.isUsable = true
                            isAnotherJob = true

                            sendParser.setExpression(
                                CareRobotMC.Eyes_Display.byte,
                                expression.toByte()
                            )
                            if (controllerPad.isUsable) {
                                Log.d("Test", "${HexDump.dumpHexString(sendParser.Data)}")
                                sendProtocolToSerial(sendParser.Data!!.clone())
                            }
                            expression++
                            if (expression >= 7) expression = 0
                            isAnotherJob = false
                        }
                        KeyEvent.KEYCODE_BUTTON_B -> {
                            controllerPad.isUsable = true
                            sharedViewModel.controlPart.value = CareRobotMC.Leg_Angle.byte
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
//                            robotModeChange(1, 1)
//                            controllerPad.isUsable = true

//                            isAnotherJob = true
//
//
//                            sendParser.ControlPosSpeed(
//                                CareRobotMC.Leg_Angle.byte,
//                                Direction.CW.direction,
//                                10f,
//                                2f
//                            )
//                            if (controllerPad.isUsable) {
//                                Log.d("Test", "${HexDump.dumpHexString(sendParser.Data)}")
//                                sendProtocolToSerial(sendParser.Data!!.clone())
//                            }
//                            isAnotherJob = false
                        }
                        KeyEvent.KEYCODE_BUTTON_L2 -> {
//                            controllerPad.isUsable = true
//                            isAnotherJob = true
//
////                            모우기
//                            sendParser.ControlPosSpeed(
//                                CareRobotMC.Leg_Angle.byte,
//                                Direction.CCW.direction,
//                                25f,
//                                2f
//                            )
//                            if (controllerPad.isUsable) {
//                                Log.d("Test", "${HexDump.dumpHexString(sendParser.Data)}")
//                                sendProtocolToSerial(sendParser.Data!!.clone())
//                            }
//                            isAnotherJob = false
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
                            for (i in 0..2) {
                                stopMotor(CareRobotMC.Leg_Angle.byte)
                                Thread.sleep(20)
                            }
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_X -> {
                            controllerPad.isUsable = false
                            for (i in 0..2) {
                                stopMotor(CareRobotMC.Left_Wheel.byte, CareRobotMC.Right_Wheel.byte)
                                Thread.sleep(20)
                            }
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_Y -> {
                            controllerPad.isUsable = false
                            for (i in 0..2) {
                                stopMotor(CareRobotMC.Waist.byte)
                                Thread.sleep(20)
                            }
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_R1 -> {
                            controllerPad.isUsable = false
                            for (i in 0..2) {
                                stopMotor(CareRobotMC.Right_Shoulder.byte)
                                Thread.sleep(20)
                            }
                            sharedViewModel.controlPart.value = null
                        }
                        KeyEvent.KEYCODE_BUTTON_L1 -> {
                            controllerPad.isUsable = false
                            for (i in 0..2) {
                                stopMotor(CareRobotMC.Left_Shoulder.byte)
                                Thread.sleep(20)
                            }
                            sharedViewModel.controlPart.value = null
                        }
//                        KeyEvent.KEYCODE_BUTTON_R2 -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.controlPart.value = null
//                        }
//                        KeyEvent.KEYCODE_BUTTON_L2 -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.controlPart.value = null
//                        }
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
            Log.d("컨트롤", "ㄴㅁㅇㄻㄴㅇㄻ")
            if (!controllerPad.isUsable) {
                stopRobot()
                return@observe
            } else {
                when (sharedViewModel.controlPart.value) {
                    CareRobotMC.Waist.byte -> {
                        val tmp = getDirectionRPM(it)
                        sharedViewModel.controlDirection = tmp.LeftDirection
                        if (waistIsUsable) {
                            isAnotherJob = true
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Waist.byte,
                                (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                                tmp.Left,
                                0.1f
                            )
                            if (controllerPad.isUsable) {
                                sendProtocolToSerial(sendParser.Data!!.clone())
                            }
                            isAnotherJob = false
                        }
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
                    CareRobotMC.Leg_Angle.byte -> {
                        isAnotherJob = true
                        val tmp = getLegDirectionRPM(it)
                        sendParser.ControlAcceleratedSpeed(
                            CareRobotMC.Leg_Angle.byte,
                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                            tmp.Left,
                            0.1f
                        )
//                        val position = pos ++
//                        sendParser.ControlPosSpeed(
//                            CareRobotMC.Leg_Angle.byte,
//                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
//                            1f,
//                            2f,
//                        )
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


    private var pos = 0f
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


    val MaxForward: Float = 40f / 5   //1326.9645
//    val MaxForward: Float = 40f   //1326.9645

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

        left = 400 * r
        right = 400 * r

        if (joy_y > 0) {
            ret.Left = left
            ret.LeftDirection = Direction.CW
        } else {
            ret.Left = left
            ret.LeftDirection = Direction.CCW
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

    private fun getLegDirectionRPM(coordinate: JoystickCoordinate): MotorRPMInfo {
        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        val joy_x = coordinate.x
        val joy_y = coordinate.y * -1f
        val r = abs(round(sqrt(joy_x.pow(2) + joy_y.pow(2)) * 100) / 100f)

//        left = pos + 0.5f
        left = 2f * r
        right = 2f * r

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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    runOnUiThread {
                        sharedViewModel.viewState.value = setView
                        loadingView.dismiss()
                    }
                }
                changeRobotPositionThread.start()



//                CoroutineScope(Dispatchers.IO).launch {
//                    val rightShoulder_tmp =
//                        sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
//                    val leftShoulder_tmp =
//                        sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
//                    val left_position = 360f - leftShoulder_tmp?.position!!
//                    val right_position = rightShoulder_tmp?.position!!
//                    async {
//                        while (true) {
//                            for (i in 0..2) {
//                                val sedate = ByteArray(12)
//                                nuriMC.ResetPostion(CareRobotMC.Left_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
//                                nuriMC.ResetPostion(CareRobotMC.Right_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 6, 0, nuriMC.Data!!.size)
//                                delay(20)
//                                val msg =
//                                    dataHandler.obtainMessage(
//                                        SerialService.MSG_SERIAL_SEND,
//                                        sedate
//                                    )
//                                dataHandler.sendMessage(msg)
//                            }
//
//                            if (sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder.byte]?.position == 0f &&
//                                sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder.byte]?.position == 0f
//                            ) {
//                                Log.d("위치제어", "브레이크걸림")
//                                break
//                            }
//                            delay(100)
//                        }
//
//                    }.await()
//
//                    async {
//                        while (true) {
//                            if ((sharedViewModel.posInfos[CareRobotMC.Left_Shoulder_Encoder.byte]!! >= 355f ||
//                                sharedViewModel.posInfos[CareRobotMC.Left_Shoulder_Encoder.byte]!! <= 5f) &&
//                                (sharedViewModel.posInfos[CareRobotMC.Right_Shoulder_Encoder.byte]!! >= 355f ||
//                                sharedViewModel.posInfos[CareRobotMC.Right_Shoulder_Encoder.byte]!! <= 5f)
//                            ) {
//                                Log.d("위치제어", "초기위치")
//                                delay(20)
//                                break
//                            } else {
//                                Log.d("위치제어", "left:${left_position}, right:${right_position}")
//
//                                val sedate = ByteArray(24)
//                                nuriMC.ControlPosSpeed(
//                                    CareRobotMC.Left_Shoulder.byte,
//                                    Direction.CCW.direction,
//                                    left_position,
//                                    1f
//                                )
//                                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
//                                nuriMC.ControlPosSpeed(
//                                    CareRobotMC.Right_Shoulder.byte,
//                                    Direction.CW.direction,
//                                    right_position,
//                                    1f
//                                )
//                                nuriMC.Data!!.copyInto(sedate, 12, 0, nuriMC.Data!!.size)
//                                val msg =
//                                    dataHandler.obtainMessage(
//                                        SerialService.MSG_SERIAL_SEND,
//                                        sedate
//                                    )
//                                dataHandler.sendMessage(msg)
//                                delay(100)
//                            }
//                        }
//                        delay(100)
//
//                    }.await()
//                    async {
//                        while (true) {
//                            for (i in 0..2) {
//                                val sedate = ByteArray(12)
//                                nuriMC.ResetPostion(CareRobotMC.Left_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
//                                nuriMC.ResetPostion(CareRobotMC.Right_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 6, 0, nuriMC.Data!!.size)
//                                delay(20)
//                                val msg =
//                                    dataHandler.obtainMessage(
//                                        SerialService.MSG_SERIAL_SEND,
//                                        sedate
//                                    )
//                                dataHandler.sendMessage(msg)
//                            }
//
//                            if (sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder.byte]?.position == 0f &&
//                                sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder.byte]?.position == 0f
//                            ) {
//                                Log.d("위치제어", "브레이크걸림")
//                                break
//                            }
//                            delay(100)
//                        }
//                        delay(100)
//
//                    }.await()
//                    runOnUiThread {
//                        sharedViewModel.viewState.value = setView
//                        loadingView.dismiss()
//                    }
//                }

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
                    runOnUiThread {
                        sharedViewModel.viewState.value = setView
                        loadingView.dismiss()
                    }
                }
                changeRobotPositionThread.start()
//                CoroutineScope(Dispatchers.IO).launch {
//                    val rightShoulder_tmp =
//                        sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
//                    val leftShoulder_tmp =
//                        sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
//                    val left_position = 360f - leftShoulder_tmp?.position!!
//                    val right_position = rightShoulder_tmp?.position!!
//                    async {
//                        while (true) {
//                            for (i in 0..2) {
//                                val sedate = ByteArray(12)
//                                nuriMC.ResetPostion(CareRobotMC.Left_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
//                                nuriMC.ResetPostion(CareRobotMC.Right_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 6, 0, nuriMC.Data!!.size)
//                                delay(20)
//                                val msg =
//                                    dataHandler.obtainMessage(
//                                        SerialService.MSG_SERIAL_SEND,
//                                        sedate
//                                    )
//                                dataHandler.sendMessage(msg)
//                            }
//
//                            if (sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder.byte]?.position == 0f &&
//                                sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder.byte]?.position == 0f
//                            ) {
//                                Log.d("위치제어", "브레이크걸림")
//                                break
//                            }
//                            delay(100)
//                        }
//
//                    }.await()
//
//                    async {
//                        while (true) {
//                            if ((sharedViewModel.posInfos[CareRobotMC.Left_Shoulder_Encoder.byte]!! >= 355f ||
//                                        sharedViewModel.posInfos[CareRobotMC.Left_Shoulder_Encoder.byte]!! <= 5f) &&
//                                (sharedViewModel.posInfos[CareRobotMC.Right_Shoulder_Encoder.byte]!! >= 355f ||
//                                        sharedViewModel.posInfos[CareRobotMC.Right_Shoulder_Encoder.byte]!! <= 5f)
//                            ) {
//                                Log.d("위치제어", "초기위치")
//                                delay(20)
//                                break
//                            } else {
//                                Log.d("위치제어", "left:${left_position}, right:${right_position}")
//
//                                val sedate = ByteArray(24)
//                                nuriMC.ControlPosSpeed(
//                                    CareRobotMC.Left_Shoulder.byte,
//                                    Direction.CCW.direction,
//                                    left_position,
//                                    1f
//                                )
//                                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
//                                nuriMC.ControlPosSpeed(
//                                    CareRobotMC.Right_Shoulder.byte,
//                                    Direction.CW.direction,
//                                    right_position,
//                                    1f
//                                )
//                                nuriMC.Data!!.copyInto(sedate, 12, 0, nuriMC.Data!!.size)
//                                val msg =
//                                    dataHandler.obtainMessage(
//                                        SerialService.MSG_SERIAL_SEND,
//                                        sedate
//                                    )
//                                dataHandler.sendMessage(msg)
//                                delay(100)
//                            }
//                        }
//                        delay(100)
//
//                    }.await()
//                    async {
//                        while (true) {
//                            for (i in 0..2) {
//                                val sedate = ByteArray(12)
//                                nuriMC.ResetPostion(CareRobotMC.Left_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
//                                nuriMC.ResetPostion(CareRobotMC.Right_Shoulder.byte)
//                                nuriMC.Data!!.copyInto(sedate, 6, 0, nuriMC.Data!!.size)
//                                delay(20)
//                                val msg =
//                                    dataHandler.obtainMessage(
//                                        SerialService.MSG_SERIAL_SEND,
//                                        sedate
//                                    )
//                                dataHandler.sendMessage(msg)
//                            }
//
//                            if (sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder.byte]?.position == 0f &&
//                                sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder.byte]?.position == 0f
//                            ) {
//                                Log.d("위치제어", "브레이크걸림")
//                                break
//                            }
//                            delay(100)
//                        }
//                        delay(100)
//
//                    }.await()
//
//                    async {
//                        while (true) {
//                            if ((sharedViewModel.posInfos[CareRobotMC.Left_Shoulder_Encoder.byte]!! >= 175 ||
//                                        sharedViewModel.posInfos[CareRobotMC.Left_Shoulder_Encoder.byte]!! <= 185f) &&
//                                (sharedViewModel.posInfos[CareRobotMC.Right_Shoulder_Encoder.byte]!! >= 175f ||
//                                        sharedViewModel.posInfos[CareRobotMC.Right_Shoulder_Encoder.byte]!! <= 185f)
//                            ) {
//                                Log.d("위치제어", "초기위치")
//                                delay(20)
//                                break
//                            } else {
//                                Log.d("위치제어", "left:${left_position}, right:${right_position}")
//
//                                val sedate = ByteArray(24)
//                                nuriMC.ControlPosSpeed(
//                                    CareRobotMC.Left_Shoulder.byte,
//                                    Direction.CW.direction,
//                                    179f,
//                                    1f
//                                )
//                                nuriMC.Data!!.copyInto(sedate, 0, 0, nuriMC.Data!!.size)
//                                nuriMC.ControlPosSpeed(
//                                    CareRobotMC.Right_Shoulder.byte,
//                                    Direction.CCW.direction,
//                                    179f,
//                                    1f
//                                )
//                                nuriMC.Data!!.copyInto(sedate, 12, 0, nuriMC.Data!!.size)
//                                val msg =
//                                    dataHandler.obtainMessage(
//                                        SerialService.MSG_SERIAL_SEND,
//                                        sedate
//                                    )
//                                dataHandler.sendMessage(msg)
//                                delay(100)
//                            }
//                        }
//                        delay(100)
//
//                    }.await()
//
//                    runOnUiThread {
//                        sharedViewModel.viewState.value = setView
//                        loadingView.dismiss()
//                    }
//                }

            }
        }

    }

    lateinit var observeWaistThread: Thread
    var isInitePosition = true
    var isSetPosition = true
    var count = 0

    lateinit var observeLegStateThread: Thread
    private var setLeg = false
    private var initLeg = false
    private fun initsetLegPosition() {
        observeLegStateThread = Thread {
            setLeg = false
            initLeg = false
            val nuriMC = NurirobotMC()
            while (true) {
                if (sharedViewModel.motorInfo[CareRobotMC.Leg_Sensor.byte] == null) {
                    Thread.sleep(1000)
                } else {
                    val first_legInfo = sharedViewModel.motorInfo[CareRobotMC.Leg_Sensor.byte]
                    Log.d("이정민", "${first_legInfo?.sensorData}")
                    Thread.sleep(5)
                    if (first_legInfo!!.sensorData == 0.toByte()) {
                        while (!initLeg) {
                            val init_legInfo =
                                sharedViewModel.motorInfo[CareRobotMC.Leg_Sensor.byte]
                            if (init_legInfo!!.sensorData == 0.toByte()) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Leg_Angle.byte,
                                    Direction.CCW.direction,
                                    0.5f,
                                    0.1f
                                )
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(15)
                            } else {
                                Log.d("이정민", "모우기 stopMotor")
                                stopMotor(CareRobotMC.Leg_Angle.byte)
                                initLeg = true
                                setLeg = true
                                break
                            }

                        }
                    } else {
                        while (!initLeg) {
                            val init_legInfo =
                                sharedViewModel.motorInfo[CareRobotMC.Leg_Sensor.byte]
                            if (init_legInfo!!.sensorData == 1.toByte()) {
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Leg_Angle.byte,
                                    Direction.CW.direction,
                                    0.5f,
                                    0.1f
                                )
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                                Thread.sleep(15)
                            } else {
                                Log.d(
                                    "이정민", "벌리기 " +
                                            "stopMotor"
                                )
                                stopMotor(CareRobotMC.Leg_Angle.byte)
                                initLeg = true
                                setLeg = true
                                break
                            }

                        }
                    }

                    if (setLeg) {
//                        nuriMC.ControlPosSpeed(
//                            CareRobotMC.Leg_Angle.byte,
//                            Direction.CCW.direction,
//                            25f,
//                            2f
//                        )
//                        val data = nuriMC.Data!!.clone()
//                        val msg =
//                            dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                        dataHandler.sendMessage(msg)
                        Thread.sleep(1000)
                        for (i in 0..10) {
                            nuriMC.ResetPostion(CareRobotMC.Leg_Angle.byte)
                            val data = nuriMC.Data!!.clone()
                            val msg =
                                dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Log.d("이정민", "두번째 끝:${i}")
                            Thread.sleep(100)
                        }
                        runOnUiThread {
                            loadingView.dismiss()
                        }
                        break
                    }

                }

            }
        }
        observeLegStateThread.start()
    }

    var feedBackMotorStateInfoThread: Thread? = null
    var isFeedBack = false
    var isAnotherJob = false
    val feedBackList = listOf<Byte>(
//        CareRobotMC.Left_Wheel.byte,
//        CareRobotMC.Right_Wheel.byte,
        CareRobotMC.Waist.byte,
        CareRobotMC.Left_Shoulder.byte,
        CareRobotMC.Right_Shoulder.byte,
        CareRobotMC.Left_Shoulder_Encoder.byte,
        CareRobotMC.Right_Shoulder_Encoder.byte,
        CareRobotMC.Waist_Sensor.byte
    )

    fun feedback() {
        feedBackMotorStateInfoThread = Thread {
            val sendParser = NurirobotMC()
            while (isFeedBack) {
                try {
                    while (isAnotherJob) {
                        Thread.sleep(10)
                    }
                    for (encorderID in feedBackList) {
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

    lateinit var initWaistThread: Thread
    private var setWaist = false
    private var initWaist = false
    private fun initWaistPosition() {
        initWaistThread = Thread {
            setWaist = false
            initWaist = false
            val nuriMC = NurirobotMC()
            while (true) {
                if (sharedViewModel.motorInfo[CareRobotMC.Waist_Sensor.byte] == null &&
                    sharedViewModel.motorInfo[CareRobotMC.Waist.byte] == null
                ) {
                    Thread.sleep(200)
                } else {
                    sharedViewModel.motorInfo[CareRobotMC.Waist_Sensor.byte]
                    Thread.sleep(5)
                    // 센서 신호가 안들어올때
                    if (sharedViewModel.motorInfo[CareRobotMC.Waist_Sensor.byte]!!.sensorData
                        == 0.toByte()
                    ) {
                        while (!initWaist) {
                            val sensorInfo =
                                sharedViewModel.motorInfo[CareRobotMC.Waist_Sensor.byte]
                            // 센서 신호가 안들어올때
                            if (sensorInfo!!.sensorData == 0.toByte()) {
                                //1. 허리를 위로 올림
                                nuriMC.ControlAcceleratedSpeed(
                                    CareRobotMC.Waist.byte,
                                    Direction.CW.direction,
                                    2f,
                                    1f
                                )
                                val data = nuriMC.Data!!.clone()
                                val msg =
                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                                dataHandler.sendMessage(msg)
                            } else {
                                //2. 센서 신호가 들어올때 모터를 멈추고 while문 빠져나감
                                Log.d("이정민", "모우기 stopMotor")
                                stopMotor(CareRobotMC.Waist.byte)
                                initWaist = true
                                break
                            }
                            Thread.sleep(20)
                        }

//                        while (initWaist){
//                            val motorInfo = sharedViewModel.motorInfo[CareRobotMC.Waist.byte]
//
//                            setWaist = true
//                        }

                    }


                    if (setWaist) {
                        Thread.sleep(1000)
                        for (i in 0..10) {
                            nuriMC.ResetPostion(CareRobotMC.Waist.byte)
                            val data = nuriMC.Data!!.clone()
                            val msg =
                                dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
                            dataHandler.sendMessage(msg)
                            Log.d("이정민", "두번째 끝:${i}")
                            Thread.sleep(100)
                        }
                        runOnUiThread {
                            loadingView.dismiss()
                        }
                        break
                    }

                }

            }
        }
        initWaistThread.start()
    }


}