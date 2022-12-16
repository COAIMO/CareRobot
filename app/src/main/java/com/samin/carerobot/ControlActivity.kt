package com.samin.carerobot

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.*
import android.hardware.usb.UsbManager
import android.os.*
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.displayhash.DisplayHashResultCallback
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.ProtocolMode
import com.jeongmin.nurimotortester.NurirobotMC
import com.samin.carerobot.Logics.*
import com.samin.carerobot.Nuri.MovementMode
import com.samin.carerobot.Service.SerialService
import com.samin.carerobot.Service.UsbSerialService
import com.samin.carerobot.databinding.ActivityControlBinding
import kotlin.math.*

class ControlActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityControlBinding
    private var feedbackThread: Thread? = null
    private var isFeedBack = false
    private lateinit var controllerPad: ControllerPad
    private lateinit var sharedPreference: SharedPreference
    private lateinit var sharedViewModel: SharedViewModel
    private val sendParser: NurirobotMC = NurirobotMC()
    var expression = 0
    lateinit var motorControllerParser: MotorControllerParser

    override fun onStart() {
        super.onStart()
        bindUsbSerialService()
    }

    override fun onResume() {
        super.onResume()
        setFilters()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mBinding = ActivityControlBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        sharedPreference = SharedPreference(this)
        motorControllerParser = MotorControllerParser(sharedViewModel)

        getGameControllerIds()
        controllerPad = ControllerPad(sharedViewModel)
        setControllerobserveThread()
//        observeWasteState()
        mBinding.btnUp.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Waist.byte] = true
            sharedViewModel.setControlpartMap[CareRobotMC.Waist_Sensor.byte] =
                true
            sendParser.ControlAcceleratedSpeed(
                CareRobotMC.Waist.byte,
                Direction.CW.direction,
                70f,
                0.1f
            )
            sharedViewModel.controlDirectionMap[CareRobotMC.Waist.byte] = Direction.CW.direction
            sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                sendParser.Data!!.clone()
        }

        mBinding.btnStop.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Waist.byte] = false
            sharedViewModel.setControlpartMap[CareRobotMC.Waist_Sensor.byte] =
                false
        }

        mBinding.btnDown.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Waist.byte] = true
            sharedViewModel.setControlpartMap[CareRobotMC.Waist_Sensor.byte] =
                true
            sendParser.ControlAcceleratedSpeed(
                CareRobotMC.Waist.byte,
                Direction.CCW.direction,
                70f,
                0.1f
            )
            sharedViewModel.controlDirectionMap[CareRobotMC.Waist.byte] = Direction.CCW.direction
            sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                sendParser.Data!!.clone()
        }
        mBinding.btnRightup.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder.byte] = true
            sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder_Encoder.byte] =
                true
            sendParser.ControlAcceleratedSpeed(
                CareRobotMC.Right_Shoulder.byte,
                Direction.CCW.direction,
                1f,
                0.1f
            )
            sharedViewModel.controlDirectionMap[CareRobotMC.Right_Shoulder.byte] = Direction.CCW.direction
            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                sendParser.Data!!.clone()
        }
        mBinding.btnRightstop.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder.byte] = false
        }
        mBinding.btnRightdown.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder.byte] = true
            sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder_Encoder.byte] =
                true
            sendParser.ControlAcceleratedSpeed(
                CareRobotMC.Right_Shoulder.byte,
                Direction.CW.direction,
                1f,
                0.1f
            )
            sharedViewModel.controlDirectionMap[CareRobotMC.Right_Shoulder.byte] = Direction.CW.direction
            sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                sendParser.Data!!.clone()
        }
        mBinding.btnLeftup.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder.byte] = true
            sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder_Encoder.byte] =
                true
            sendParser.ControlAcceleratedSpeed(
                CareRobotMC.Left_Shoulder.byte,
                Direction.CW.direction,
                1f,
                0.1f
            )
            sharedViewModel.controlDirectionMap[CareRobotMC.Left_Shoulder.byte] = Direction.CW.direction
            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                sendParser.Data!!.clone()
        }
        mBinding.btnLeftstop.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder.byte] = false
        }
        mBinding.btnLeftdown.setOnClickListener {
            sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder.byte] = true
            sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder_Encoder.byte] =
                true
            sendParser.ControlAcceleratedSpeed(
                CareRobotMC.Left_Shoulder.byte,
                Direction.CCW.direction,
                1f,
                0.1f
            )
            sharedViewModel.controlDirectionMap[CareRobotMC.Left_Shoulder.byte] = Direction.CCW.direction
            sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                sendParser.Data!!.clone()
        }

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

    override fun onDestroy() {
        super.onDestroy()
        stopfeedback()
        stopControllerobserve()
        unbindUsbSerialService()
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
                    unbindUsbSerialService()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {

                }
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {

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
                UsbSerialService.SERIALPORT_READY -> {
                    isFeedBack = true
                    feedback()
//                    observeMotorState()
//                    observeWasteState()
                }
                UsbSerialService.MSG_STOP_ROBOT -> {
//                    for (i in 0..2) {
//                        stopRobot()
//                    }
                }
                UsbSerialService.MSG_MOVE_ROBOT -> {

                }
                UsbSerialService.MSG_SET_SPEED -> {

                }
                UsbSerialService.MSG_ERROR -> {

                }
                UsbSerialService.MSG_ROBOT_SERIAL_SEND -> {
                    sendProtocolToRobot(msg.obj as ByteArray)
                    Log.d("send", "handler_send : ${HexDump.toHexString(msg.obj as ByteArray)}\t")
                }
                ProtocolMode.FEEDPos.byte.toInt() -> {
                    motorControllerParser.parser(msg.obj as ByteArray)
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun sendProtocolToRobot(data: ByteArray) {
        val msg = Message.obtain(null, UsbSerialService.MSG_ROBOT_SERIAL_SEND)
        val bundle = Bundle()
        bundle.putByteArray("", data)
        msg.data = bundle
        usbSerialService?.send(msg)
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

    val feedbackallList = listOf<Byte>(
        CareRobotMC.Left_Shoulder_Encoder.byte,
        CareRobotMC.Right_Shoulder_Encoder.byte,
        CareRobotMC.Waist_Sensor.byte,
        CareRobotMC.Waist.byte,
        CareRobotMC.Left_Shoulder.byte,
        CareRobotMC.Right_Shoulder.byte,
        CareRobotMC.Left_Wheel.byte,
        CareRobotMC.Right_Wheel.byte,
    )

    fun stopfeedback() {
        isFeedBack = false
        feedbackThread?.interrupt()
        feedbackThread?.join()
        feedbackThread = null
    }

    fun feedback() {
        feedbackThread = Thread {
            val sendParser = NurirobotMC()
            try {
                while (isFeedBack) {
                    // 이벤트 발생 시 사용할 피드백 리스트 필터
                    val feedbacklst = feedbackallList.filter {
                        sharedViewModel.setControlpartMap[it] == true
                    }
                    Log.d("feedback", "${feedbacklst}")
                    // 피드백리스트가 있을 경우 사용할 것만 피드백
                    if (!feedbacklst.isEmpty()) {
                        for (i in feedbacklst) {
                            sendParser.Feedback(i, ProtocolMode.REQPos.byte)
                            val data = sendParser.Data!!.clone()
                            val msg =
                                usbSerialHandler.obtainMessage(
                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                    data
                                )
                            usbSerialHandler.sendMessage(msg)
                            Thread.sleep(50)
                        }
                    } else {
                        // 이벤트가 없을 시 전체 피드백
                        for (i in feedbackallList) {
                            sendParser.Feedback(i, ProtocolMode.REQPos.byte)
                            val data = sendParser.Data!!.clone()
                            val msg =
                                usbSerialHandler.obtainMessage(
                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                    data
                                )
                            usbSerialHandler.sendMessage(msg)
                            Thread.sleep(50)
                        }
                    }

                    for (i in sharedViewModel.sendProtocolMap) {
                        // 움직이고자 하는 부분이 있는 경우
                        if (sharedViewModel.setControlpartMap[i.key] == true) {
                            when (i.key) {
                                CareRobotMC.Waist.byte -> {
                                    when (sharedViewModel.waistStateMap[CareRobotMC.Waist_Sensor.byte]) {
                                        // 위쪽 센서가 걸렸을때
                                        1 -> {
                                            if (i.value[6] == Direction.CCW.direction) {
                                                val msg =
                                                    usbSerialHandler.obtainMessage(
                                                        UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                        i.value
                                                    )
                                                usbSerialHandler.sendMessage(msg)
                                            } else {
                                                sharedViewModel.setControlpartMap[i.key] = false
                                            }
                                        }
                                        4 -> {
                                            if (i.value[6] == Direction.CW.direction) {
                                                val msg =
                                                    usbSerialHandler.obtainMessage(
                                                        UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                        i.value
                                                    )
                                                usbSerialHandler.sendMessage(msg)
                                            } else {
                                                sharedViewModel.setControlpartMap[i.key] = false

                                            }
                                        }
                                        5 -> {
                                            sharedViewModel.setControlpartMap[i.key] = false
                                        }
                                        0 -> {
                                            val msg =
                                                usbSerialHandler.obtainMessage(
                                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                    i.value
                                                )
                                            usbSerialHandler.sendMessage(msg)
                                        }
                                    }

                                }
                                CareRobotMC.Right_Shoulder.byte -> {
                                    val tmp =
                                        sharedViewModel.motorInfo[CareRobotMC.Right_Shoulder_Encoder.byte]
                                    if (tmp?.min_Alert == true && tmp.max_Alert == false) {
                                        Log.d("오른쪽어깨", "위로 최소알람걸림")
                                        Log.d("오른쪽어깨", "${i.value[6]}")
                                        if (i.value[6] == Direction.CCW.direction) {
                                            sharedViewModel.setControlpartMap[i.key] = false
                                        } else {
                                            sharedViewModel.setControlpartMap[i.key] = true
                                            val msg =
                                                usbSerialHandler.obtainMessage(
                                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                    i.value
                                                )
                                            usbSerialHandler.sendMessage(msg)
                                        }

                                    } else if (tmp?.max_Alert == true && tmp.min_Alert == false) {
                                        Log.d("오른쪽어깨", "최대알람걸림")
                                        Log.d("오른쪽어깨", "${i.value[6]}")
                                        if (i.value[6] == Direction.CW.direction) {
                                            Log.d("오른쪽어깨", "멈춤멈춤")

                                            sharedViewModel.setControlpartMap[i.key] = false
                                        } else {
                                            Log.d("오른쪽어깨", "진행진행")
                                            val msg =
                                                usbSerialHandler.obtainMessage(
                                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                    i.value
                                                )
                                            usbSerialHandler.sendMessage(msg)
                                        }

                                    } else {
                                        Log.d("오른쪽어깨", "알람 아닐 경우")
                                        val msg =
                                            usbSerialHandler.obtainMessage(
                                                UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                i.value
                                            )
                                        usbSerialHandler.sendMessage(msg)
                                    }
                                }
                                CareRobotMC.Left_Shoulder.byte -> {
                                    val tmp =
                                        sharedViewModel.motorInfo[CareRobotMC.Left_Shoulder_Encoder.byte]
                                    if (tmp?.max_Alert == true && tmp.min_Alert == false) {
                                        if (i.value[6] == Direction.CW.direction) {
                                            sharedViewModel.setControlpartMap[i.key] = false
                                        } else {
                                            sharedViewModel.setControlpartMap[i.key] = true
                                            val msg =
                                                usbSerialHandler.obtainMessage(
                                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                    i.value
                                                )
                                            usbSerialHandler.sendMessage(msg)
                                        }

                                    } else if (tmp?.min_Alert == true && tmp.max_Alert == false) {
                                        if (i.value[6] == Direction.CCW.direction) {
                                            sharedViewModel.setControlpartMap[i.key] = false
                                        } else {
                                            val msg =
                                                usbSerialHandler.obtainMessage(
                                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                    i.value
                                                )
                                            usbSerialHandler.sendMessage(msg)
                                        }

                                    } else {
                                        val msg =
                                            usbSerialHandler.obtainMessage(
                                                UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                                i.value
                                            )
                                        usbSerialHandler.sendMessage(msg)
                                    }
                                }
                                else -> {
                                    val msg =
                                        usbSerialHandler.obtainMessage(
                                            UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                            i.value
                                        )
                                    usbSerialHandler.sendMessage(msg)
                                }
                            }
//                            val msg =
//                                usbSerialHandler.obtainMessage(
//                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
//                                    i.value
//                                )
//                            usbSerialHandler.sendMessage(msg)
                            Log.d("command", "${HexDump.toHexString(i.value)}")
//                            Thread.sleep(30)
                        } else {
                            //움직이고자 하는 부분이 없고
                            if (!sharedViewModel.motorInfo[i.key]!!.isStop) {
                                //로봇이 움직이고 있는 부분이 있으면 멈춤
                                sendParser.ControlAcceleratedSpeed(
                                    i.key,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                Log.d("stop_send", "${HexDump.toHexString(i.value)}")
                                val data = sendParser.Data!!.clone()
                                val msg =
                                    usbSerialHandler.obtainMessage(
                                        UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                        data
                                    )
                                usbSerialHandler.sendMessage(msg)
//                                Thread.sleep(30)
                            }
                        }
                        Thread.sleep(50)

                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        feedbackThread?.start()
    }

    var controllerEvent: MotionEvent? = null
    var controllerobserveThread: Thread? = null
    var isObserve = false
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return if (ControllerPad.isJoyStick(event!!)) {
            controllerEvent = event
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }

    private fun setControllerobserveThread() {
        isObserve = true
        controllerobserveThread = Thread {
            while (isObserve) {
                try {
                    if (controllerEvent != null) {
                        controllerPad.processJoystickInput(controllerEvent!!, -1)
                        if (controllerPad.isUsable) {


                            if (sharedViewModel.setControlpartMap[CareRobotMC.Left_Wheel.byte] == true &&
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Wheel.byte] == true
                            ) {
                                val tmpRPM = getRPMMath(sharedViewModel.left_Joystick.value!!)
                                moveWheelchair(tmpRPM)
                            } else if (sharedViewModel.setControlpartMap[CareRobotMC.Waist.byte] == true) {
                                val tmp = getDirectionRPM(sharedViewModel.right_Joystick.value!!)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Waist.byte,
                                    (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                                    tmp.Left,
                                    0.1f
                                )
                                sharedViewModel.controlDirectionMap[CareRobotMC.Waist.byte] = tmp.LeftDirection.direction
                                sharedViewModel.motorIsStopMap[CareRobotMC.Waist.byte] =
                                    tmp.Left == 0f
                                sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                    sendParser.Data!!.clone()
                            } else if (sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder.byte] == true) {
                                val tmp =
                                    getShoulderDirectionRPM(sharedViewModel.right_Joystick.value!!)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    (if (tmp.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
                                    tmp.Right,
                                    0.1f
                                )
                                sharedViewModel.controlDirectionMap[CareRobotMC.Waist.byte] = tmp.LeftDirection.direction
                                sharedViewModel.motorIsStopMap[CareRobotMC.Right_Shoulder.byte] =
                                    tmp.Right == 0f
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                    sendParser.Data!!.clone()

                            } else if (sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder.byte] == true) {
                                val tmp =
                                    getShoulderDirectionRPM(sharedViewModel.right_Joystick.value!!)
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    (if (tmp.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
                                    tmp.Left,
                                    0.1f
                                )
                                sharedViewModel.controlDirectionMap[CareRobotMC.Waist.byte] = tmp.LeftDirection.direction
                                sharedViewModel.motorIsStopMap[CareRobotMC.Left_Shoulder.byte] =
                                    tmp.Left == 0f
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                    sendParser.Data!!.clone()
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

    fun stopControllerobserve() {
        isFeedBack = false
        controllerobserveThread?.interrupt()
        controllerobserveThread?.join()
        controllerobserveThread = null
    }

    private fun isFireKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
            val eventAction = event.action
            val keyCode = event.keyCode
            var handled = false
            if (ControllerPad.isGamePad(event)) {
                Log.d("컨트롤", "무시된다 ${keyCode} ${eventAction}")
                if (eventAction == KeyEvent.ACTION_UP) {
                    if (event.repeatCount == 0) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_BUTTON_A -> {
                                controllerPad.isUsable = false
                                sharedViewModel.setControlpartMap[CareRobotMC.Eyes_Display.byte] =
                                    false
                            }
                            KeyEvent.KEYCODE_BUTTON_X -> {
                                controllerPad.isUsable = false
                                sharedViewModel.setControlpartMap[CareRobotMC.Left_Wheel.byte] =
                                    false
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Wheel.byte] =
                                    false
                            }
                            KeyEvent.KEYCODE_DEL -> {
                                controllerPad.isUsable = false
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
                                sharedViewModel.setControlpartMap[CareRobotMC.Left_Wheel.byte] =
                                    false
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Wheel.byte] =
                                    false
                            }
                            KeyEvent.KEYCODE_BUTTON_Y -> {
                                controllerPad.isUsable = false
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Waist.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Waist.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.setControlpartMap[CareRobotMC.Waist.byte] = false
                                sharedViewModel.setControlpartMap[CareRobotMC.Waist_Sensor.byte] =
                                    false
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                controllerPad.isUsable = false
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Right_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Right_Shoulder.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder.byte] =
                                    false
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder_Encoder.byte] =
                                    false
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                controllerPad.isUsable = false
                                sendParser.ControlAcceleratedSpeed(
                                    CareRobotMC.Left_Shoulder.byte,
                                    Direction.CCW.direction,
                                    0f,
                                    0.1f
                                )
                                sharedViewModel.sendProtocolMap[CareRobotMC.Left_Shoulder.byte] =
                                    sendParser.Data!!.clone()
                                sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder.byte] =
                                    false
                                sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder_Encoder.byte] =
                                    false
                            }
                            KeyEvent.KEYCODE_BACK -> {
                                return false
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
                                sharedViewModel.setControlpartMap[CareRobotMC.Eyes_Display.byte] =
                                    true
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
                            }
                            KeyEvent.KEYCODE_BUTTON_X -> {
                                controllerPad.isUsable = true
                                sharedViewModel.setControlpartMap[CareRobotMC.Left_Wheel.byte] =
                                    true
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Wheel.byte] =
                                    true
                            }
                            KeyEvent.KEYCODE_BUTTON_Y -> {
                                controllerPad.isUsable = true
                                sharedViewModel.setControlpartMap[CareRobotMC.Waist.byte] = true
                                sharedViewModel.setControlpartMap[CareRobotMC.Waist_Sensor.byte] =
                                    true
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                controllerPad.isUsable = true
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder.byte] =
                                    true
                                sharedViewModel.setControlpartMap[CareRobotMC.Right_Shoulder_Encoder.byte] =
                                    true
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                controllerPad.isUsable = true
                                sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder.byte] =
                                    true
                                sharedViewModel.setControlpartMap[CareRobotMC.Left_Shoulder_Encoder.byte] =
                                    true
                            }
                            KeyEvent.KEYCODE_BACK -> {
                                return false
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

        }
        return super.dispatchKeyEvent(event)
    }

    private fun getRPMMath(coordinate: JoystickCoordinate): MotorRPMInfo {
        val ret: MotorRPMInfo = MotorRPMInfo()
        var left = 0f
        var right = 0f

        val joy_x = coordinate.x
        val joy_y = coordinate.y * -1f
        val angle = Math.toDegrees(atan2(joy_y, joy_x).toDouble())
        val radian = angle * Math.PI / 180f
        var r = abs(round(sqrt(joy_x.pow(2) + joy_y.pow(2)) * 100) / 100f)
        Log.d(
            MainActivity.TAG,
            "x: $joy_x\tY: $joy_y\t anlge: $angle\t r: $r"
        )
        if (r > 1f)
            r = 1f

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
            MainActivity.TAG,
            "left: $left\t right : $right"
        )

        ret.Left = abs(left)
        ret.Right = abs(right)
        Log.d(
            MainActivity.TAG,
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

    private fun moveWheelchair(tmpRPMInfo: MotorRPMInfo) {
        val sedate = ByteArray(20)
        sendParser.ControlAcceleratedSpeed(
            CareRobotMC.Left_Wheel.byte,
            (if (tmpRPMInfo.LeftDirection == Direction.CW) 0x01 else 0x00).toByte(),
            tmpRPMInfo.Left,
            calcConcentrationWheel(tmpRPMInfo.Left)
        )
        sharedViewModel.controlDirectionMap[CareRobotMC.Left_Wheel.byte] = tmpRPMInfo.LeftDirection.direction
        sharedViewModel.motorIsStopMap[CareRobotMC.Left_Wheel.byte] = tmpRPMInfo.Left == 0f
        sharedViewModel.sendProtocolMap[CareRobotMC.Left_Wheel.byte] = sendParser.Data!!.clone()
        sendParser.ControlAcceleratedSpeed(
            CareRobotMC.Right_Wheel.byte,
            (if (tmpRPMInfo.RightDirection == Direction.CW) 0x01 else 0x00).toByte(),
            tmpRPMInfo.Right,
            calcConcentrationWheel(tmpRPMInfo.Right)
        )
        sharedViewModel.controlDirectionMap[CareRobotMC.Right_Wheel.byte] = tmpRPMInfo.RightDirection.direction
        sharedViewModel.motorIsStopMap[CareRobotMC.Right_Wheel.byte] = tmpRPMInfo.Right == 0f
        sharedViewModel.sendProtocolMap[CareRobotMC.Right_Wheel.byte] = sendParser.Data!!.clone()
    }

    private fun calcConcentrationWheel(curr: Float): Float {
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

}