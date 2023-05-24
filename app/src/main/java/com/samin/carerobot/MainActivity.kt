package com.samin.carerobot

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.*
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.*
import android.provider.Settings
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
import com.samin.carerobot.Nuri.MovementMode
import com.samin.carerobot.Service.SerialService
import com.samin.carerobot.Service.UsbSerialService
import com.samin.carerobot.databinding.ActivityMainBinding
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    lateinit var mBinding: ActivityMainBinding
    lateinit var loginFragment: LoginFragment
    lateinit var newAccountFragment: NewAccountFragment
    lateinit var mainFragment: MainFragment
    lateinit var heartDialogFragment: HeartDialogFragment
    lateinit var sharedPreference: SharedPreference
    lateinit var controllerPad: ControllerPad
    private lateinit var sharedViewModel: SharedViewModel
    lateinit var motorControllerParser: MotorControllerParser
    var waistIsUsable = false
    var leftShoulderIsUsable = false
    var rightShoulderIsUsable = false
    var isWheelStop = true

    // 휠 컨트롤 여부 플레그
    var isWheelControl = false

    companion object {
        val TAG = "태그"
    }
    val maxcurrent = 400
    val maxcurrentunlimit = 500
    var isbtn: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        } else {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        sharedPreference = SharedPreference(this)
        motorControllerParser = MotorControllerParser(sharedViewModel)
        controllerPad = ControllerPad(sharedViewModel)
        setFragment()
        checkLogin()
        moveRobot()
        loadingView = LoadingDialog(this)
        getGameControllerIds()
        setControllerobserveThread()
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
//        bindMessengerService()
        bindUsbSerialService()
    }

    override fun onResume() {
        super.onResume()
        setFilters()
    }

    override fun onDestroy() {
        super.onDestroy()
//        unbindMessengerService()
        stopfeedback()
        unbindUsbSerialService()
    }

    private fun setFragment() {
        loginFragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.HostFragment_container, loginFragment).commit()
        newAccountFragment = NewAccountFragment()
        mainFragment = MainFragment()
        heartDialogFragment = HeartDialogFragment()
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
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Toast.makeText(
                        context,
                        "USB가 해제 되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
//                    unbindUsbSerialService()
//                    android.os.Process.killProcess(android.os.Process.myPid())
                    Handler(Looper.getMainLooper()).postDelayed({
                        //실행할 코드
                        unbindUsbSerialService()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }, 3000)
                }
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
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        registerReceiver(broadcastReceiver, filter)
    }

    private val usbSerialHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UsbSerialService.SERIALPORT_INITIALIZE ->{
                    loadingView.show()
                }
                UsbSerialService.SERIALPORT_READY -> {
                    isFeedBack = true
//                    Thread.sleep(500)


                    Handler(Looper.getMainLooper()).postDelayed({
                        //실행할 코드
                        feedback()
                        observeMotorState()
                        observeWasteState()
                        loadingView.dismiss()
                    }, 500)
                }
                UsbSerialService.MSG_STOP_ROBOT -> {
                    for (i in 0..2) {
                        stopRobot()
                    }
                }
                UsbSerialService.MSG_MOVE_ROBOT -> {
                    when (msg.obj as Byte) {
                        MovementMode.GO_forward.byte -> {
                            sharedViewModel.isControl.set(CareRobotMC.Wheel.byte.toInt())
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Wheel.byte,
                                Direction.CW.direction,
                                sharedViewModel.wheelMaxSpeed,
                                3f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] =
                                sendParser.Data!!.clone()
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Wheel.byte,
                                Direction.CCW.direction,
                                sharedViewModel.wheelMaxSpeed,
                                3f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.GO_backward.byte -> {
                            sharedViewModel.isControl.set(CareRobotMC.Wheel.byte.toInt())
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Wheel.byte,
                                Direction.CCW.direction,
                                sharedViewModel.wheelMaxSpeed / 2,
                                2f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] =
                                sendParser.Data!!.clone()
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Wheel.byte,
                                Direction.CW.direction,
                                sharedViewModel.wheelMaxSpeed / 2,
                                2f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.TURN_Left.byte -> {
                            sharedViewModel.isControl.set(CareRobotMC.Wheel.byte.toInt())
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Wheel.byte,
                                Direction.CW.direction,
                                sharedViewModel.wheelMaxSpeed / 7,
                                0.1f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] =
                                sendParser.Data!!.clone()
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Wheel.byte,
                                Direction.CW.direction,
                                sharedViewModel.wheelMaxSpeed / 7,
                                0.1f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.TURN_Right.byte -> {
                            sharedViewModel.isControl.set(CareRobotMC.Wheel.byte.toInt())
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Wheel.byte,
                                Direction.CCW.direction,
                                sharedViewModel.wheelMaxSpeed / 7,
                                0.1f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] =
                                sendParser.Data!!.clone()
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Wheel.byte,
                                Direction.CCW.direction,
                                sharedViewModel.wheelMaxSpeed / 7,
                                0.1f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.UP_Lift.byte -> {
                            sharedViewModel.isControl.set(CareRobotMC.Waist_Sensor.byte.toInt())
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Waist.byte,
                                Direction.CW.direction,
                                70f,
                                0.1f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.DOWN_Lift.byte -> {
                            sharedViewModel.isControl.set(CareRobotMC.Waist_Sensor.byte.toInt())
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Waist.byte,
                                Direction.CCW.direction,
                                70f,
                                0.1f
                            )
                            sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.LEFT_SHOULDER_UP.byte -> {
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Shoulder.byte,
                                Direction.CW.direction,
                                1f,
                                1.1f
                            )
                            sharedViewModel.leftControlDirection = Direction.CW
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.LEFT_SHOULDER_DOWN.byte -> {
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Shoulder.byte,
                                Direction.CCW.direction,
                                1f,
                                1.1f
                            )
                            sharedViewModel.leftControlDirection = Direction.CCW
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.RIGHT_SHOULDER_UP.byte -> {
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Shoulder.byte,
                                Direction.CCW.direction,
                                1f,
                                1.1f
                            )
                            sharedViewModel.rightControlDirection = Direction.CCW
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                sendParser.Data!!.clone()
                        }
                        MovementMode.RIGHT_SHOULDER_DOWN.byte -> {
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Shoulder.byte,
                                Direction.CW.direction,
                                1f,
                                1.1f
                            )
                            sharedViewModel.rightControlDirection = Direction.CW
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                sendParser.Data!!.clone()
                        }

                    }

                }
                UsbSerialService.MSG_SET_SPEED -> {
                    when (msg.obj as Byte) {
                        0x00.toByte() -> {
                            sharedViewModel.wheelMaxSpeed = WheelSpeed.First.speed
                        }
                        0x01.toByte() -> {
                            sharedViewModel.wheelMaxSpeed = WheelSpeed.Second.speed
                        }
                    }
                }
                UsbSerialService.MSG_ERROR -> {
                    Toast.makeText(
                        this@MainActivity,
                        "USB가 정상적으로 연결되었는지 확인해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        //실행할 코드
                        unbindUsbSerialService()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }, 3000)

//                    Thread.sleep(5000)
//                    unbindUsbSerialService()
//                    android.os.Process.killProcess(android.os.Process.myPid())
                }
                UsbSerialService.MSG_ROBOT_SERIAL_SEND -> {
                    sendProtocolToRobot(msg.obj as ByteArray)
//                    Log.d("send", "handler_send : ${HexDump.toHexString(msg.obj as ByteArray)}\t")
                }
                ProtocolMode.FEEDPos.byte.toInt() -> {
                    motorControllerParser.parser(msg.obj as ByteArray)
                }
                UsbSerialService.MSG_PC_PROTOCOL ->{
                    val data = msg.obj as ByteArray
                    sharedViewModel.pcProtocol.value = HexDump.toHexString(data)
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    private val usbSerialClient = Messenger(usbSerialHandler)
    private var usbSerialService: Messenger? = null
    private val usbSerialServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            usbSerialService = Messenger(service).apply {
                send(Message.obtain(null, UsbSerialService.MSG_BIND_CLIENT, 0, 0).apply {
                    replyTo = usbSerialClient
                })
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            usbSerialService = null
        }
    }

    private fun bindUsbSerialService() {
        Intent(this, UsbSerialService::class.java).run {
            bindService(this, usbSerialServiceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    private fun unbindUsbSerialService() {
        usbSerialService?.send(
            Message.obtain(null, UsbSerialService.MSG_UNBIND_CLIENT, 0, 0).apply {
                replyTo = usbSerialClient
            })
        unbindService(usbSerialServiceConnection)
    }

    private val serialSVCIPCHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SerialService.MSG_SERIAL_CONNECT -> {
//                    loadingView.show()
//                    isFeedBack = true
//                    feedback()
//                    Thread.sleep(1000)
//                    observeMotorState()
//                    observeWasteState()
//                    Thread.sleep(1000)
//                    initWaistPosition()
//                    initsetLegPosition()

                }
//                ProtocolMode.FEEDPos.byte.toInt() -> {
//                    motorControllerParser.parser(msg.obj as ByteArray)
//                }
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
//                                    Log.d(
//                                        TEST,
//                                        "Left_Shoulder_Encoder : ${value.position} sensor: ${value.proximity_Sensor}"
//                                    )
                                    if (value.max_Alert!! || value.min_Alert!!) {
//                                        val msg = dataHandler.obtainMessage(
//                                            SerialService.MSG_STOP_MOTOR,
//                                            CareRobotMC.Left_Shoulder.byte
//                                        )
//                                        dataHandler.sendMessage(msg)
                                        stopMotor(CareRobotMC.Left_Shoulder.byte)
                                        Log.d("정지정지", "Left_Shoulder_Encoder : 멈춤")
                                    } else {
                                    }
                                }
                                CareRobotMC.Right_Shoulder_Encoder.byte -> {
//                                    Log.d(
//                                        TEST,
//                                        "Right_Shoulder_Encoder : ${value.position} sensor: ${value.proximity_Sensor}"
//                                    )
                                    if (value.max_Alert!! || value.min_Alert!!) {
//                                        val msg = dataHandler.obtainMessage(
//                                            SerialService.MSG_STOP_MOTOR,
//                                            CareRobotMC.Right_Shoulder.byte
//                                        )
//                                        dataHandler.sendMessage(msg)
                                        stopMotor(CareRobotMC.Right_Shoulder.byte)

                                    } else {
                                    }
                                }
//                                CareRobotMC.Waist_Sensor.byte -> {
////                                    value.sensorData == 1.toByte() || value.sensorData == 2.toByte() || value.sensorData == 3.toByte()
//                                    // 센서 위에꺼만 들어올때
//                                    Log.d(
//                                        "허리센서 ",
//                                        "value:${value.sensorData}, 방향:${sharedViewModel.controlDirection}"
//                                    )
//                                    if (value.sensorData == 1.toByte()) {
//                                        if (sharedViewModel.controlDirection == Direction.CW) {
//                                            waistIsUsable = false
//                                            Log.d("허리센서 ", "상단 정지신호")
//                                            val msg = dataHandler.obtainMessage(
//                                                SerialService.MSG_STOP_MOTOR,
//                                                CareRobotMC.Waist.byte
//                                            )
//                                            dataHandler.sendMessage(msg)
//                                        } else {
//                                            waistIsUsable = true
//                                        }
//
////                                        value.sensorData == 4.toByte() || value.sensorData == 8.toByte() || value.sensorData == 12.toByte()
//                                        // 센서 아래꺼만 들어왔을때
//                                    } else if (value.sensorData == 4.toByte()) {
//                                        if (sharedViewModel.controlDirection == Direction.CCW) {
//                                            waistIsUsable = false
//                                            Log.d("허리센서 ", "하단 정지신호")
//                                            val msg = dataHandler.obtainMessage(
//                                                SerialService.MSG_STOP_MOTOR,
//                                                CareRobotMC.Waist.byte
//                                            )
//                                            dataHandler.sendMessage(msg)
//                                        } else {
//                                            waistIsUsable = true
//                                        }
//                                        //센서 위아래 동시에 들어올때
//                                    } else if (value.sensorData == 5.toByte()) {
//                                        waistIsUsable = false
//                                        val msg = dataHandler.obtainMessage(
//                                            SerialService.MSG_STOP_MOTOR,
//                                            CareRobotMC.Waist.byte
//                                        )
//                                        dataHandler.sendMessage(msg)
//                                    } else {
//                                        waistIsUsable = true
//                                    }
//
//                                }
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

    lateinit var observeWasteStateThread: Thread

    private fun observeWasteState() {
        observeWasteStateThread = Thread {
            while (true) {
                try {
//                    if (!checkEncorder()) {
//                        Thread.sleep(1000)
//                    }
                    if (sharedViewModel.motorInfo.isNullOrEmpty()) {
                        Thread.sleep(1000)
                        sharedViewModel.waistStateMap[CareRobotMC.Waist_Sensor.byte] = 5
                    } else {
                        for ((key, value) in sharedViewModel.waistStateMap) {
                            if (sharedViewModel.waistlstRecvTime[key] != null) {
                                if (sharedViewModel.waistlstRecvTime[key]!! + 800 < System.currentTimeMillis()) {
                                    sharedViewModel.waistStateMap[CareRobotMC.Waist_Sensor.byte] = 5
                                }
                            } else {
                                sharedViewModel.waistStateMap[CareRobotMC.Waist_Sensor.byte] = 5
                            }

                            when (key) {
                                CareRobotMC.Waist_Sensor.byte -> {
//                                    Log.d("허리센서", "${value}")
                                    if (value == 1) {
                                        if (sharedViewModel.waistControlDirection == Direction.CW) {
                                            waistIsUsable = false
                                            sendParser.ControlAcceleratedSpeed(
                                                CareRobotMC.Waist.byte,
                                                Direction.CCW.direction,
                                                0f,
                                                0.1f
                                            )
                                            val data = sendParser.Data!!.clone()
                                            sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                                data
                                        } else {
                                            waistIsUsable = true
                                        }

                                    } else if (value == 4) {
                                        if (sharedViewModel.waistControlDirection == Direction.CCW) {
                                            waistIsUsable = false
                                            sendParser.ControlAcceleratedSpeed(
                                                CareRobotMC.Waist.byte,
                                                Direction.CCW.direction,
                                                0f,
                                                0.1f
                                            )
                                            val data = sendParser.Data!!.clone()
                                            sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                                data
                                        } else {
                                            waistIsUsable = true
                                        }
                                        //센서 위아래 동시에 들어올때
                                    } else if (value == 5) {
                                        waistIsUsable = false
                                        sendParser.ControlAcceleratedSpeed(
                                            CareRobotMC.Waist.byte,
                                            Direction.CCW.direction,
                                            0f,
                                            0.1f
                                        )
                                        val data = sendParser.Data!!.clone()
                                        sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                            data
                                    } else {
                                        waistIsUsable = true
                                    }

                                }
                                CareRobotMC.Waist.byte -> {
                                    Log.d("허리속도 ", "${value}")

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
        observeWasteStateThread.start()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return if (ControllerPad.isJoyStick(event!!)) {
            // Process the movements starting from the
            // earliest historical position in the batch
//            (0 until event.historySize).forEach { i ->
//                // Process the event at historical position i
//                Log.d("tag","$i")
//                controllerPad.processJoystickInput(event, i)
//                controllerEvent = event
//            }
            // Process the current movement sample in the batch (position -1)
//            controllerPad.processJoystickInput(event, -1)
            controllerEvent = event

            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }

    var controllerEvent: MotionEvent? = null
    var controllerobserveThread: Thread? = null
    private fun setControllerobserveThread() {
        controllerobserveThread = Thread {
            while (true) {
                try {
                    if (controllerEvent != null) {
                        controllerPad.processJoystickInput(controllerEvent!!, -1)
                        if (!controllerPad.isUsable) {
                        } else {
                            if (sharedViewModel.controlPart.value == CareRobotMC.Wheel.byte) {
                                val tmpRPM = getRPMMath(sharedViewModel.left_Joystick.value!!)
                                isWheelStop = tmpRPM.Right == 0f || tmpRPM.Left == 0f
                                moveWheelchair(tmpRPM)
                            } else {
                                for (i in 0..2) {

                                    stopMotor(
                                        CareRobotMC.Left_Wheel.byte,
                                        CareRobotMC.Right_Wheel.byte
                                    )
                                    Thread.sleep(20)
                                }
                            }
                        }

                    }

                    Thread.sleep(80)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        controllerobserveThread?.start()
    }


    var expression = 0

//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if (event != null) {
//            if (ControllerPad.isGamePad(event)) {
//                if (event.repeatCount == 0) {
//                    when (keyCode) {
//                        KeyEvent.KEYCODE_BUTTON_A -> {
//                            controllerPad.isUsable = true
//                            isAnotherJob = true
//
//                            sendParser.setExpression(
//                                CareRobotMC.Eyes_Display.byte,
//                                expression.toByte()
//                            )
//                            if (controllerPad.isUsable) {
////                                Log.d("Test", "${HexDump.dumpHexString(sendParser.Data)}")
//                                sharedViewModel.sendProtocolMap[CareRobotMC.Eyes_Display.byte] =
//                                    sendParser.Data!!.clone()
////                                sendProtocolToSerial(sendParser.Data!!.clone())
//                            }
//                            expression++
//                            if (expression >= 7) expression = 0
//                            isAnotherJob = false
//                        }
//                        KeyEvent.KEYCODE_BUTTON_B -> {
//                            controllerPad.isUsable = true
//                            sharedViewModel.isControl.set(CareRobotMC.Leg_Angle.byte.toInt())
//                            sharedViewModel.controlPart.value = CareRobotMC.Leg_Angle.byte
//                        }
//                        KeyEvent.KEYCODE_BUTTON_X -> {
//                            controllerPad.isUsable = true
//                            sharedViewModel.isControl.set(CareRobotMC.Wheel.byte.toInt())
//                            sharedViewModel.controlPart.value = CareRobotMC.Wheel.byte
//                        }
//                        KeyEvent.KEYCODE_BUTTON_Y -> {
//                            controllerPad.isUsable = true
//                            sharedViewModel.isControl.set(CareRobotMC.Waist_Sensor.byte.toInt())
//                            sharedViewModel.controlPart.value = CareRobotMC.Waist.byte
//                        }
//                        KeyEvent.KEYCODE_BUTTON_R1 -> {
//                            controllerPad.isUsable = true
//                            sharedViewModel.isControl.set(CareRobotMC.Right_Shoulder.byte.toInt())
//                            sharedViewModel.controlPart.value = CareRobotMC.Right_Shoulder.byte
//                        }
//                        KeyEvent.KEYCODE_BUTTON_L1 -> {
//                            controllerPad.isUsable = true
//                            sharedViewModel.isControl.set(CareRobotMC.Left_Shoulder.byte.toInt())
//                            sharedViewModel.controlPart.value = CareRobotMC.Left_Shoulder.byte
//                        }
//                        KeyEvent.KEYCODE_BUTTON_R2 -> {
////                            robotModeChange(1, 1)
////                            controllerPad.isUsable = true
//
////                            isAnotherJob = true
////
////
////                            sendParser.ControlPosSpeed(
////                                CareRobotMC.Leg_Angle.byte,
////                                Direction.CW.direction,
////                                10f,
////                                2f
////                            )
////                            if (controllerPad.isUsable) {
////                                Log.d("Test", "${HexDump.dumpHexString(sendParser.Data)}")
////                                sendProtocolToSerial(sendParser.Data!!.clone())
////                            }
////                            isAnotherJob = false
//                        }
//                        KeyEvent.KEYCODE_BUTTON_L2 -> {
////                            controllerPad.isUsable = true
////                            isAnotherJob = true
////
//////                            모우기
////                            sendParser.ControlPosSpeed(
////                                CareRobotMC.Leg_Angle.byte,
////                                Direction.CCW.direction,
////                                25f,
////                                2f
////                            )
////                            if (controllerPad.isUsable) {
////                                Log.d("Test", "${HexDump.dumpHexString(sendParser.Data)}")
////                                sendProtocolToSerial(sendParser.Data!!.clone())
////                            }
////                            isAnotherJob = false
//                        }
//                        else -> {
//                            keyCode.takeIf { isFireKey(it) }?.run {
////                                handled = true
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return super.onKeyDown(keyCode, event)
//    }
//
//    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
//        if (event != null) {
//            var handled = false
//            if (ControllerPad.isGamePad(event)) {
//                if (event.repeatCount == 0) {
//                    when (keyCode) {
//                        KeyEvent.KEYCODE_BUTTON_A -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.isControl.set(0)
//                            sharedViewModel.controlPart.value = null
//                        }
//                        KeyEvent.KEYCODE_BUTTON_B -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.isControl.set(0)
////                            for (i in 0..2) {
////                                stopMotor(CareRobotMC.Leg_Angle.byte)
////                                Thread.sleep(20)
////                            }
////                            sendParser.ControlAcceleratedSpeed(CareRobotMC.Leg_Angle.byte, Direction.CCW.direction, 0f, 0.1f)
////                            sharedViewModel.sendProtocolMap[CareRobotMC.Leg_Angle.byte] = sendParser.Data!!.clone()
//                            sharedViewModel.controlPart.value = null
//                        }
//                        KeyEvent.KEYCODE_BUTTON_X -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.isControl.set(0)
////                            for (i in 0..2) {
////                                stopMotor(CareRobotMC.Left_Wheel.byte, CareRobotMC.Right_Wheel.byte)
////                                Thread.sleep(20)
////                            }
//                            sendParser.ControlAcceleratedSpeed(
//                                CareRobotMC.Left_Wheel.byte,
//                                Direction.CCW.direction,
//                                0f,
//                                0.1f
//                            )
//                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] =
//                                sendParser.Data!!.clone()
//                            sendParser.ControlAcceleratedSpeed(
//                                CareRobotMC.Right_Wheel.byte,
//                                Direction.CCW.direction,
//                                0f,
//                                0.1f
//                            )
//                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] =
//                                sendParser.Data!!.clone()
//                            sharedViewModel.controlPart.value = null
//                        }
//                        KeyEvent.KEYCODE_BUTTON_Y -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.isControl.set(0)
////                            for (i in 0..2) {
////                                stopMotor(CareRobotMC.Waist.byte)
////                                Thread.sleep(20)
////                            }
//                            sendParser.ControlAcceleratedSpeed(
//                                CareRobotMC.Waist.byte,
//                                Direction.CCW.direction,
//                                0f,
//                                0.1f
//                            )
//                            sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
//                                sendParser.Data!!.clone()
//                            sharedViewModel.controlPart.value = null
//                        }
//                        KeyEvent.KEYCODE_BUTTON_R1 -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.isControl.set(0)
////                            for (i in 0..2) {
////                                stopMotor(CareRobotMC.Right_Shoulder.byte)
////                                Thread.sleep(20)
////                            }
//                            sendParser.ControlAcceleratedSpeed(
//                                CareRobotMC.Right_Shoulder.byte,
//                                Direction.CCW.direction,
//                                0f,
//                                0.1f
//                            )
//                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
//                                sendParser.Data!!.clone()
//                            sharedViewModel.controlPart.value = null
//                        }
//                        KeyEvent.KEYCODE_BUTTON_L1 -> {
//                            controllerPad.isUsable = false
//                            sharedViewModel.isControl.set(0)
////                            for (i in 0..2) {
////                                stopMotor(CareRobotMC.Left_Shoulder.byte)
////                                Thread.sleep(20)
////                            }
//                            sendParser.ControlAcceleratedSpeed(
//                                CareRobotMC.Left_Shoulder.byte,
//                                Direction.CCW.direction,
//                                0f,
//                                0.1f
//                            )
//                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
//                                sendParser.Data!!.clone()
//                            sharedViewModel.controlPart.value = null
//                        }
////                        KeyEvent.KEYCODE_BUTTON_R2 -> {
////                            controllerPad.isUsable = false
////                            sharedViewModel.controlPart.value = null
////                        }
////                        KeyEvent.KEYCODE_BUTTON_L2 -> {
////                            controllerPad.isUsable = false
////                            sharedViewModel.controlPart.value = null
////                        }
//                        else -> {
//                            keyCode.takeIf { isFireKey(it) }?.run {
//                                handled = true
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return super.onKeyUp(keyCode, event)
//    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
            val eventAction = event.action
            val keyCode = event.keyCode
            var handled = false
            if (ControllerPad.isGamePad(event)) {
//                Log.d("컨트롤", "무시된다 ${keyCode} ${eventAction}")
                if (eventAction == KeyEvent.ACTION_UP) {
                    if (event.repeatCount == 0) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_BUTTON_A -> {
                                controllerPad.isUsable = false
                                sharedViewModel.isControl.set(0)
                                sharedViewModel.controlPart.value = null
                            }
                            KeyEvent.KEYCODE_BUTTON_B -> {
                                controllerPad.isUsable = false
                                sharedViewModel.isControl.set(0)
                                sharedViewModel.controlPart.value = null
                                isbtn.set(!isbtn.get())
                            }
                            KeyEvent.KEYCODE_BUTTON_X -> {
                                controllerPad.isUsable = false
                                isWheelStop = true
                                isWheelControl = false
                                sharedViewModel.isControl.set(0)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Wheel.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] =
                                    sendParser.Data!!.clone()
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Wheel.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.controlPart.value = null
                            }
                            KeyEvent.KEYCODE_DEL -> {
                                controllerPad.isUsable = false
                                sharedViewModel.isControl.set(0)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Wheel.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] =
                                    sendParser.Data!!.clone()
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Wheel.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.controlPart.value = null
                            }
                            KeyEvent.KEYCODE_BUTTON_Y -> {
                                controllerPad.isUsable = false
                                sharedViewModel.isControl.set(0)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Waist.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.controlPart.value = null
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                controllerPad.isUsable = false
                                sharedViewModel.isControl.set(0)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.controlPart.value = null
                                sharedViewModel.currentMax[CareRobotMC.Right_Shoulder.byte] = 0
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                controllerPad.isUsable = false
                                sharedViewModel.isControl.set(0)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.controlPart.value = null
                                sharedViewModel.currentMax[CareRobotMC.Left_Shoulder.byte] = 0
                            }
                            else -> {
                                keyCode.takeIf { isFireKey(it) }?.run {
                                    handled = true
                                }
                            }
                        }
                    }

                } else if (eventAction == KeyEvent.ACTION_DOWN) {
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
                                    sharedViewModel.sendProtocolMap[CareRobotMC.Eyes_Display.byte] =
                                        sendParser.Data!!.clone()
                                }
                                expression++
                                if (expression >= 7) expression = 0
                                isAnotherJob = false
                            }
                            KeyEvent.KEYCODE_BUTTON_B -> {
                                controllerPad.isUsable = true
                                sharedViewModel.isControl.set(CareRobotMC.Leg_Angle.byte.toInt())
                                sharedViewModel.controlPart.value = CareRobotMC.Leg_Angle.byte
                            }
                            KeyEvent.KEYCODE_BUTTON_X -> {
                                controllerPad.isUsable = true
                                isWheelStop = false
                                isWheelControl = true
                                sharedViewModel.isControl.set(CareRobotMC.Wheel.byte.toInt())
                                sharedViewModel.controlPart.value = CareRobotMC.Wheel.byte
                            }
                            KeyEvent.KEYCODE_BUTTON_Y -> {
                                controllerPad.isUsable = true
                                sharedViewModel.isControl.set(CareRobotMC.Waist_Sensor.byte.toInt())
                                sharedViewModel.controlPart.value = CareRobotMC.Waist.byte
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                controllerPad.isUsable = true
                                sharedViewModel.isControl.set(CareRobotMC.Right_Shoulder.byte.toInt())
                                sharedViewModel.controlPart.value = CareRobotMC.Right_Shoulder.byte
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                controllerPad.isUsable = true
                                sharedViewModel.isControl.set(CareRobotMC.Left_Shoulder.byte.toInt())
                                sharedViewModel.controlPart.value = CareRobotMC.Left_Shoulder.byte
                            }
                            KeyEvent.KEYCODE_BUTTON_R2 -> {

                            }
                            KeyEvent.KEYCODE_BUTTON_L2 -> {

                            }
                            else -> {
                                keyCode.takeIf { isFireKey(it) }?.run {
                                    handled = true
                                }
                            }
                        }
                    }

                }
                return true
            }

        }
        return super.dispatchKeyEvent(event)
    }

    private fun isFireKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A

    private fun moveRobot() {

//        sharedViewModel.left_Joystick.observe(this) {
////            if (!controllerPad.isUsable) {
////                stopRobot()
////                return@observe
////            } else {
////                if (sharedViewModel.controlPart.value == CareRobotMC.Wheel.byte) {
////                    val tmpRPM = getRPMMath(it)
////                    moveWheelchair(tmpRPM)
////                } else {
////                    for (i in 0..2) {
////                        stopMotor(CareRobotMC.Left_Wheel.byte, CareRobotMC.Right_Wheel.byte)
////                        Thread.sleep(20)
////                    }
////                }
////            }
//            if (!controllerPad.isUsable) {
//                return@observe
//            } else {
//                if (sharedViewModel.controlPart.value == CareRobotMC.Wheel.byte) {
//                    val tmpRPM = getRPMMath(it)
//                    moveWheelchair(tmpRPM)
//                } else {
//                    for (i in 0..2) {
//                        stopMotor(CareRobotMC.Left_Wheel.byte, CareRobotMC.Right_Wheel.byte)
//                        Thread.sleep(20)
//                    }
//                }
//            }
//        }

        sharedViewModel.right_Joystick.observe(this) {
//            Log.d("컨트롤", "ㄴㅁㅇㄻㄴㅇㄻ")
            if (!controllerPad.isUsable) {
//                stopRobot()
                return@observe
            } else {
                when (sharedViewModel.controlPart.value) {
                    CareRobotMC.Waist.byte -> {
                        val tmp = getDirectionRPM(it)
                        sharedViewModel.waistControlDirection = tmp.LeftDirection
                        if (waistIsUsable) {
//                            isAnotherJob = true
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Waist.byte,
                                (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                                tmp.Left,
                                0.1f
                            )


                            if (controllerPad.isUsable) {
//                                sendProtocolToSerial(sendParser.Data!!.clone())
                                sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                    sendParser.Data!!.clone()
                            }
//                            isAnotherJob = false
                        }
                    }
                    CareRobotMC.Right_Shoulder.byte -> {
//                        isAnotherJob = true
                        sharedViewModel.currentMax[CareRobotMC.Right_Shoulder.byte]?.let {
                            if (isbtn.get() && it > maxcurrent) {
                                return@observe
                            }
                        }

                        if (sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]?.position != null) {

                            val tmp = getShoulderDirectionRPM(it)
                            sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]?.position?.let {
                                if (it in 180f..270f) {
                                    if (tmp.RightDirection == Direction.CCW)
                                        return@observe

                                    Log.d(
                                        "오른쪽 CCW제한",
                                        "${sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]?.position}"
                                    )
                                } else if (it in 270f..360f) {
                                    if (tmp.RightDirection == Direction.CW)
                                        return@observe

                                    Log.d(
                                        "오른쪽 CW제한",
                                        "${sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]?.position}"
                                    )
                                } else {

                                }
                            }
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Shoulder.byte,
                                (if (tmp.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
                                tmp.Right,
                                2.1f
                            )
                            sharedViewModel.rightControlDirection = tmp.RightDirection
                            if (controllerPad.isUsable) {
//                            sendProtocolToSerial(sendParser.Data!!.clone())
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                    sendParser.Data!!.clone()
                            }
                        }
//                        isAnotherJob = false
                    }
                    CareRobotMC.Left_Shoulder.byte -> {
//                        isAnotherJob = true

                        sharedViewModel.currentMax[CareRobotMC.Left_Shoulder.byte]?.let {
                            if (isbtn.get() && it > maxcurrent) {
                                return@observe
                            }
                        }

//                        val tmp = getShoulderDirectionRPM(it)
//                        sendParser.ControlAcceleratedSpeed(
//                            CareRobotMC.Left_Shoulder.byte,
//                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
//                            tmp.Left,
//                            0.1f
//                        )
//                        sharedViewModel.leftControlDirection = tmp.LeftDirection
//                        if (controllerPad.isUsable) {
////                            sendProtocolToSerial(sendParser.Data!!.clone())
//                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
//                                sendParser.Data!!.clone()
//                        }

                        if (sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]?.position != null) {
                            val tmp = getShoulderDirectionRPM(it)
                            sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]?.position?.let {
                                if (it in 90f..180f) {
                                    if (tmp.RightDirection == Direction.CCW)
                                        return@observe

                                    Log.d(
                                        "왼쪽 CCW제한",
                                        "${sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]?.position}"
                                    )
                                } else if (it in 0f..90f) {
                                    if (tmp.RightDirection == Direction.CW)
                                        return@observe

                                    Log.d(
                                        "왼쪽 CW제한",
                                        "${sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]?.position}"
                                    )
                                } else {

                                }
                            }
                            sendParser.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Shoulder.byte,
                                (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                                tmp.Left,
                                2.1f
                            )
                            sharedViewModel.leftControlDirection = tmp.LeftDirection
                            if (controllerPad.isUsable) {
//                            sendProtocolToSerial(sendParser.Data!!.clone())
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                    sendParser.Data!!.clone()
                            }
                        }
                    }
                    CareRobotMC.Leg_Angle.byte -> {
//                        isAnotherJob = true
//                        val tmp = getLegDirectionRPM(it)
//                        sendParser.ControlAcceleratedSpeed(
//                            CareRobotMC.Leg_Angle.byte,
//                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
//                            tmp.Left,
//                            0.1f
//                        )
////                        val position = pos ++
////                        sendParser.ControlPosSpeed(
////                            CareRobotMC.Leg_Angle.byte,
////                            (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
////                            1f,
////                            2f,
////                        )
//                        sharedViewModel.controlDirection = tmp.LeftDirection
//                        if (controllerPad.isUsable) {
//                            sendProtocolToSerial(sendParser.Data!!.clone())
//                        }
//                        isAnotherJob = false
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

//        sendParser.Data!!.copyInto(sedate, 0, 0, sendParser.Data!!.size)
        sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] = sendParser.Data!!.clone()
        sendParser.ControlAcceleratedSpeed(
            CareRobotMC.Right_Wheel.byte,
            (if (tmpRPMInfo.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
            tmpRPMInfo.Right,
            calcConcentrationRight(tmpRPMInfo.Right)
        )
//        sendParser.Data!!.copyInto(sedate, 10, 0, sendParser.Data!!.size)
//        sendProtocolToSerial(sedate)
        sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] = sendParser.Data!!.clone()

    }


    //원래 속도
//    val MaxForward: Float = 40f / 5   //1326.9645
    //분당 약 18m
//    var `sharedViewModel.wheelMaxSpeed`: Float = 29f   //1326.9645

    //기존 방향과 반대
    //TODO
    private fun getRPMMath(coordinate: JoystickCoordinate): MotorRPMInfo {
        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        var joy_x = coordinate.x
        var joy_y = coordinate.y * -1f
        val angle = Math.toDegrees(atan2(joy_y, joy_x).toDouble())
        val radian = angle * Math.PI / 180f
        var r = abs(round(sqrt(joy_x.pow(2) + joy_y.pow(2)) * 100) / 100f)
        Log.d(
            TAG,
            "x: $joy_x\tY: $joy_y\t anlge: $angle\t r: $r"
        )
        if (r > 1f) {
            r = 0f
            joy_x = 0f
            joy_y = 0f
        }

        if (joy_x > 0 && joy_y > 0) {
            //우회전
            left = sharedViewModel.wheelMaxSpeed * r * joy_x
            right = sharedViewModel.wheelMaxSpeed * r * joy_x * 0.75f
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        } else if (joy_x > 0 && joy_y < 0) {
            //후진 우회전
            left = -1 * sharedViewModel.wheelMaxSpeed / 2 * r * joy_x
            right = -1 * sharedViewModel.wheelMaxSpeed / 2 * r * joy_x * 0.75f
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else if (joy_x < 0 && joy_y > 0) {
            //좌회전
            left = sharedViewModel.wheelMaxSpeed * r * joy_x * 0.75f
            right = sharedViewModel.wheelMaxSpeed * r * joy_x
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        } else if (joy_x < 0 && joy_y < 0) {
            //후진 좌회전
            left = -1 * sharedViewModel.wheelMaxSpeed / 2 * r * joy_x * 0.75f
            right = -1 * sharedViewModel.wheelMaxSpeed / 2 * r * joy_x
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else if (joy_x == 0f && joy_y > 0) {
            //전진
            left = sharedViewModel.wheelMaxSpeed * r
            right = sharedViewModel.wheelMaxSpeed * r
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CW
        } else if (joy_x == 0f && joy_y < 0) {
            //후진
            left = sharedViewModel.wheelMaxSpeed * r * 0.66f
            right = sharedViewModel.wheelMaxSpeed * r * 0.66f
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CCW
        } else if (joy_y == 0f && joy_x > 0) {
            //제자리 우회전
            left = sharedViewModel.wheelMaxSpeed * r / 7
            right = sharedViewModel.wheelMaxSpeed * r / 7
            ret.LeftDirection = Direction.CCW
            ret.RightDirection = Direction.CCW
        } else if (joy_y == 0f && joy_x < 0) {
            //제자리 좌회전
            left = sharedViewModel.wheelMaxSpeed * r / 7
            right = sharedViewModel.wheelMaxSpeed * r / 7
            ret.LeftDirection = Direction.CW
            ret.RightDirection = Direction.CW
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
        for (count in 0..2) {
            if (id_2 == null) {
                sendParser.ControlAcceleratedSpeed(id_1, Direction.CCW.direction, 0f, 0.1f)
                sharedViewModel.sendProtocolMap[id_1] = sendParser.Data!!.clone()
            } else {
                sendParser.ControlAcceleratedSpeed(id_1, Direction.CCW.direction, 0f, 0.1f)
                sharedViewModel.sendProtocolMap[id_1] = sendParser.Data!!.clone()

//                Log.d("바퀴","왼 ${HexDump.toHexString(sendParser.Data!!.clone())}")
                sendParser.ControlAcceleratedSpeed(id_2, Direction.CCW.direction, 0f, 0.1f)
                sharedViewModel.sendProtocolMap[id_2] = sendParser.Data!!.clone()
//                Log.d("바퀴","오 ${HexDump.toHexString(sendParser.Data!!.clone())}")

            }
            sharedViewModel.currentMax[id_1] = 0
            id_2?.let {
                sharedViewModel.currentMax[it] = 0
            }
        }
    }

    val stopList = listOf<Byte>(
        CareRobotMC.Right_Wheel.byte,
        CareRobotMC.Left_Wheel.byte,
        CareRobotMC.Waist.byte,
        CareRobotMC.Left_Shoulder.byte,
        CareRobotMC.Right_Shoulder.byte
    )

    fun stopRobot() {
//        for (i in CareRobotMC.Left_Shoulder.byte..CareRobotMC.Left_Wheel.byte) {
//            sendParser.ControlAcceleratedSpeed(i.toByte(), Direction.CCW.direction, 0f, 0.1f)
//            sharedViewModel.sendProtocolMap[i.toByte()] = sendParser.Data!!.clone()
////            sendProtocolToSerial(nuriMC.Data!!.clone())
////            Thread.sleep(20)
//        }

        for (i in stopList) {
            sendParser.ControlAcceleratedSpeed(i.toByte(), Direction.CCW.direction, 0f, 0.1f)
            sharedViewModel.sendProtocolMap[i.toByte()] = sendParser.Data!!.clone()

//            sendProtocolToSerial(nuriMC.Data!!.clone())
//            Thread.sleep(20)
        }
    }


    private lateinit var changeRobotPositionThread: Thread
    lateinit var loadingView: LoadingDialog

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
                            if (rightShoulder_tmp?.position!! > 2 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 80) {
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
                                sharedViewModel.rightControlDirection = Direction.CW
                                val data = nuriMC.Data!!.clone()
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                    data
//                                val msg =
//                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                                dataHandler.sendMessage(msg)
                                Thread.sleep(40)
                            } else if (rightShoulder_tmp.min_Range!! < rightShoulder_tmp.position!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!! - 80) {
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
                                sharedViewModel.rightControlDirection = Direction.CCW
                                val data = nuriMC.Data!!.clone()
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                    data
//                                val msg =
//                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                                dataHandler.sendMessage(msg)
                                Thread.sleep(40)
                            } else if (rightShoulder_tmp.position!! in 0.0..2.0) {
                                if (!right_ShoulderSet) {
//                                    val msg = dataHandler.obtainMessage(
//                                        SerialService.MSG_STOP_MOTOR,
//                                        CareRobotMC.Right_Shoulder.byte
//                                    )
//                                    dataHandler.sendMessage(msg)
                                    stopMotor(CareRobotMC.Right_Shoulder.byte)
                                    Thread.sleep(40)
                                }
                                right_ShoulderSet = true
                            }

                            if (leftShoulder_tmp?.position!! > 2 && leftShoulder_tmp.position!! <= leftShoulder_tmp.min_Range!! + 80) {
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
                                sharedViewModel.leftControlDirection = Direction.CW
                                val data = nuriMC.Data!!.clone()
//                                val msg =
//                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                                dataHandler.sendMessage(msg)
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                    data
                                Thread.sleep(40)
                            } else if (leftShoulder_tmp.position!! > leftShoulder_tmp.min_Range!! && leftShoulder_tmp.position!! > leftShoulder_tmp.max_Range!! - 80) {
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
                                sharedViewModel.leftControlDirection = Direction.CCW

//                                val msg =
//                                    dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                                dataHandler.sendMessage(msg)
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                    data
                                Thread.sleep(40)
                            } else if (leftShoulder_tmp.position!! in 0.0..2.0) {
                                if (!left_ShoulderSet) {
//                                    val msg = dataHandler.obtainMessage(
//                                        SerialService.MSG_STOP_MOTOR,
//                                        CareRobotMC.Left_Shoulder.byte
//                                    )
//                                    dataHandler.sendMessage(msg)
                                    stopMotor(CareRobotMC.Left_Shoulder.byte)
                                    Thread.sleep(40)
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
            }

            2 -> {
                changeRobotPositionThread = Thread {
                    while (step_1) {

                        val rightShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
                        val leftShoulder_tmp =
                            sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
                        if (rightShoulder_tmp?.position!! > 181 && rightShoulder_tmp.position!! <= rightShoulder_tmp.min_Range!! + 80) {
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
                            sharedViewModel.rightControlDirection = Direction.CW
                            val data = nuriMC.Data!!.clone()
//                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                            dataHandler.sendMessage(msg)
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                data
//                            Thread.sleep(20)
                        } else if (rightShoulder_tmp.position!! > rightShoulder_tmp.min_Range!! && rightShoulder_tmp.position!! > rightShoulder_tmp.max_Range!! - 80) {
                            right_ShoulderSet = false
                            nuriMC.ControlAcceleratedSpeed(
                                CareRobotMC.Right_Shoulder.byte,
                                Direction.CCW.direction,
                                1f,
                                0.1f
                            )
                            val data = nuriMC.Data!!.clone()
//                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                            dataHandler.sendMessage(msg)
//                            Thread.sleep(20)
                            sharedViewModel.rightControlDirection = Direction.CCW

                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                data
                            Thread.sleep(40)
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
                            sharedViewModel.rightControlDirection = Direction.CCW

                            val data = nuriMC.Data!!.clone()
//                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                            dataHandler.sendMessage(msg)
//                            Thread.sleep(20)
                            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                data
                            Thread.sleep(40)
                        } else if (rightShoulder_tmp.position!! in 179.0..181.0) {
                            if (right_ShoulderSet) {
//                                val msg = dataHandler.obtainMessage(
//                                    SerialService.MSG_STOP_MOTOR,
//                                    CareRobotMC.Right_Shoulder.byte
//                                )
//                                dataHandler.sendMessage(msg)
                                stopMotor(CareRobotMC.Right_Shoulder.byte)
                                Thread.sleep(40)
                            }
                            right_ShoulderSet = true
                        }

                        if (leftShoulder_tmp?.position!! < 179 && leftShoulder_tmp.position!! >= leftShoulder_tmp.max_Range!! - 80) {
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
                            sharedViewModel.leftControlDirection = Direction.CCW

                            val data = nuriMC.Data!!.clone()
//                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                            dataHandler.sendMessage(msg)
//                            Thread.sleep(20)
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                data
                            Thread.sleep(40)
                        } else if (leftShoulder_tmp.position!! < leftShoulder_tmp.min_Range!! + 80) {
                            left_ShoulderSet = false
                            nuriMC.ControlAcceleratedSpeed(
                                CareRobotMC.Left_Shoulder.byte,
                                Direction.CW.direction,
                                2f,
                                0.1f
                            )
                            sharedViewModel.leftControlDirection = Direction.CW

                            val data = nuriMC.Data!!.clone()
//                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                            dataHandler.sendMessage(msg)
//                            Thread.sleep(20)
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                data
                            Thread.sleep(40)
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
                            sharedViewModel.leftControlDirection = Direction.CW

                            val data = nuriMC.Data!!.clone()
//                            val msg = dataHandler.obtainMessage(SerialService.MSG_SERIAL_SEND, data)
//                            dataHandler.sendMessage(msg)
//                            Thread.sleep(20)
                            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                data
                            Thread.sleep(40)
                        } else if (leftShoulder_tmp.position!! in 179.0..181.0) {
                            if (!left_ShoulderSet) {
//                                val msg = dataHandler.obtainMessage(
//                                    SerialService.MSG_STOP_MOTOR,
//                                    CareRobotMC.Left_Shoulder.byte
//                                )
//                                dataHandler.sendMessage(msg)
                                stopMotor(CareRobotMC.Left_Shoulder.byte)
                                Thread.sleep(40)
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
            }
        }

    }

    fun cancelRobotChange() {
        stopRobot()
        changeRobotPositionThread.interrupt()
        changeRobotPositionThread.join()
    }

    var feedBackMotorStateInfoThread: Thread? = null
    var isFeedBack = false
    var isAnotherJob = false
    val feedBackList = listOf<Byte>(
//        CareRobotMC.Left_Wheel.byte,
//        CareRobotMC.Right_Wheel.byte,
//        CareRobotMC.Left_Shoulder.byte,
//        CareRobotMC.Right_Shoulder.byte,
        CareRobotMC.Left_Shoulder_Encoder.byte,
        CareRobotMC.Right_Shoulder_Encoder.byte
    )
    val feedbackWaistList = listOf<Byte>(
        CareRobotMC.Waist.byte,
        CareRobotMC.Waist_Sensor.byte
    )

    val feedbackallList = listOf<Byte>(
        CareRobotMC.Left_Shoulder_Encoder.byte,
        CareRobotMC.Right_Shoulder_Encoder.byte,
        CareRobotMC.Waist_Sensor.byte,
        CareRobotMC.Right_Wheel.byte,
        CareRobotMC.Left_Wheel.byte
    )

    val wheelLst = listOf<Byte>(
        CareRobotMC.Right_Wheel.byte,
        CareRobotMC.Left_Wheel.byte
    )

    val topLst = listOf<Byte>(
        CareRobotMC.Left_Shoulder_Encoder.byte,
        CareRobotMC.Right_Shoulder_Encoder.byte,
        CareRobotMC.Waist_Sensor.byte,
    )

    val mcfeeds = listOf<Byte>(
        CareRobotMC.Left_Shoulder_Encoder.byte,
        CareRobotMC.Left_Shoulder.byte,
        CareRobotMC.Right_Shoulder_Encoder.byte,
        CareRobotMC.Right_Shoulder.byte,
        CareRobotMC.Waist_Sensor.byte
    )

    val mcfeedarmss = listOf<Byte>(
        CareRobotMC.Left_Shoulder.byte,
        CareRobotMC.Right_Shoulder.byte,
    )

    fun feedback() {
        feedBackMotorStateInfoThread = Thread {
            val sendParser = NurirobotMC()
/*            val limitsenseorsprotocols = topLst.map {
                sendParser.Feedback(it, ProtocolMode.REQPos.byte)
                sendParser.Data!!.clone()
            }*/
            val motorCtrlFeed = mcfeeds.map {
                sendParser.Feedback(it, ProtocolMode.REQPos.byte)
                sendParser.Data!!.clone()
            }

            val motorarmCtrlFeed = mcfeedarmss.map {
                sendParser.Feedback(it, ProtocolMode.REQPos.byte)
                sendParser.Data!!.clone()
            }

            while (isFeedBack) {
                try {
                    while (isAnotherJob) {
                        Thread.sleep(30)
                    }

                    if (!isWheelControl) {
                        if (isbtn.get()) {
                            for (t in motorarmCtrlFeed) {
                                val msg =
                                    usbSerialHandler.obtainMessage(
                                        UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                        t
                                    )
                                usbSerialHandler.sendMessage(msg)
                                Thread.sleep(30)
                            }
                        }
                        else {
                            for (t in motorCtrlFeed) {
                                val msg =
                                    usbSerialHandler.obtainMessage(
                                        UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                        t
                                    )
                                usbSerialHandler.sendMessage(msg)
                                Thread.sleep(30)
                            }
                        }
                    }

                    for (i in sharedViewModel.sendProtocolMap) {
                            if (isWheelControl) {
                                if (i.key == CareRobotMC.Right_Wheel.byte || i.key == CareRobotMC.Left_Wheel.byte) {
                                    val msg =
                                        usbSerialHandler.obtainMessage(
                                            UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                            i.value
                                        )
                                    usbSerialHandler.sendMessage(msg)
                                }
                            } else {
                                var tmpsendvalue = i.value
                                if (i.key != CareRobotMC.Right_Wheel.byte || i.key != CareRobotMC.Left_Wheel.byte) {

                                    if (i.key == 1.toByte()) {
//                                        Log.d("1번", "${sharedViewModel.motorInfo[11]} ${i.value[6]} ${i.value[7]} ${i.value[8]}");

                                        sharedViewModel.currentMax[1]?.let {
                                            if (isbtn.get() && it > maxcurrent) {
                                                sendParser.ControlAcceleratedSpeed(1, Direction.CCW.direction, 0f, 0.1f)
                                                tmpsendvalue = sendParser.Data!!.clone()
                                            }
                                        }

                                        if (sharedViewModel.motorInfo[11]?.position!! in 90f..180f) {
                                            if(i.value[6] == 1.toByte()) {

                                                if (!(i.value[7] == 0.toByte() && i.value[8] == 0.toByte())) {
                                                    Thread.sleep(5)
                                                    continue
                                                }
                                            }
                                        }
                                        else if (sharedViewModel.motorInfo[11]?.position!! in 0f..90f) {
                                            if(i.value[6] == 0.toByte()) {
                                                if (!(i.value[7] == 0.toByte() && i.value[8] == 0.toByte())) {
                                                    Thread.sleep(5)
                                                    continue
                                                }
                                            }
                                        }
                                    }

                                    if (i.key == 3.toByte()) {
//                                        Log.d("1번", "${sharedViewModel.motorInfo[13]} ${i.value[6]} ${i.value[7]} ${i.value[8]}");
                                        sharedViewModel.currentMax[3]?.let {
                                            if (isbtn.get() && it > maxcurrent) {
                                                sendParser.ControlAcceleratedSpeed(3, Direction.CCW.direction, 0f, 0.1f)
                                                tmpsendvalue = sendParser.Data!!.clone()
                                            }
                                        }
                                        if (sharedViewModel.motorInfo[13]?.position!! in 180f..270f) {
                                            if(i.value[6] == 0.toByte()) {

                                                if (!(i.value[7] == 0.toByte() && i.value[8] == 0.toByte())) {
                                                    Thread.sleep(5)
                                                    continue
                                                }
                                            }
                                        }
                                        else if (sharedViewModel.motorInfo[13]?.position!! in 270f..360f) {
                                            if(i.value[6] == 1.toByte()) {
                                                if (!(i.value[7] == 0.toByte() && i.value[8] == 0.toByte())) {
                                                    Thread.sleep(5)
                                                    continue
                                                }
                                            }
                                        }
                                    }

                                    if (i.key == 5.toByte()) {
                                        Log.d("5번", "${sharedViewModel.motorInfo[15]} ${i.value[6]} ${i.value[7]} ${i.value[8]}");

                                        if (sharedViewModel.motorInfo[15]?.sensorData == 1.toByte() ||
                                            sharedViewModel.motorInfo[15]?.sensorData == 5.toByte()) {
                                            if(i.value[6] == 1.toByte()) {

                                                if (!(i.value[7] == 0.toByte() && i.value[8] == 0.toByte())) {
                                                    Thread.sleep(5)
                                                    continue
                                                }
                                            }
                                        }
                                        else if (sharedViewModel.motorInfo[15]?.sensorData == 4.toByte() ||
                                            sharedViewModel.motorInfo[15]?.sensorData == 5.toByte()) {
                                            if(i.value[6] == 0.toByte()) {
                                                if (!(i.value[7] == 0.toByte() && i.value[8] == 0.toByte())) {
                                                    Thread.sleep(5)
                                                    continue
                                                }
                                            }
                                        }
                                    }

                                    val msg =
                                        usbSerialHandler.obtainMessage(
                                            UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                            tmpsendvalue
                                        )
                                    usbSerialHandler.sendMessage(msg)
                                }
                            }
//                            Log.d("보내는거 다를경우", "${HexDump.toHexString(i.value)}")
                        Thread.sleep(5)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Log.d("dead", "dead")
        }
        feedBackMotorStateInfoThread?.start()
    }

    fun stopfeedback() {
        isFeedBack = false
        feedBackMotorStateInfoThread?.join()
        feedBackMotorStateInfoThread?.interrupt()
        feedBackMotorStateInfoThread = null
    }

    fun showHeartAnimation() {
        heartDialogFragment.show(supportFragmentManager, "")
    }

    fun sendProtocolToPC(data: ByteArray) {
        val msg = Message.obtain(null, UsbSerialService.MSG_PC_SERIAL_SEND)
        val bundle = Bundle()
        bundle.putByteArray("", data)
        msg.data = bundle
        usbSerialService?.send(msg)
    }

    fun sendProtocolToRobot(data: ByteArray) {
        val msg = Message.obtain(null, UsbSerialService.MSG_ROBOT_SERIAL_SEND)
        val bundle = Bundle()
        bundle.putByteArray("", data)
        msg.data = bundle
        usbSerialService?.send(msg)
    }


}