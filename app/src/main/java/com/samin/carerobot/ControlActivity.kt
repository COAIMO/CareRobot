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

class ControlActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityControlBinding
    private var feedbackThread: Thread? = null
    private var isFeedBack = false
    private lateinit var controllerPad: ControllerPad
    private lateinit var sharedPreference: SharedPreference
    private lateinit var sharedViewModel: SharedViewModel
    private val sendParser: NurirobotMC = NurirobotMC()
    var expression = 0

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
        getGameControllerIds()
        controllerPad = ControllerPad(sharedViewModel)

        mBinding.btnBtn.setOnClickListener {

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

                }
                ProtocolMode.FEEDPos.byte.toInt() -> {
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

    fun feedback() {
        feedbackThread = Thread {
            val sendParser = NurirobotMC()
            try {
                while (isFeedBack) {
                    if (sharedViewModel.sendProtocolMap.isEmpty()) {
                        for (i in feedbackallList) {
                            sendParser.Feedback(i, ProtocolMode.REQPos.byte)
                            val data = sendParser.Data!!.clone()
                            val msg =
                                usbSerialHandler.obtainMessage(
                                    UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                    data
                                )
                            usbSerialHandler.sendMessage(msg)
                            Log.d(
                                "feedback",
                                "handler_send : ${HexDump.toHexString(msg.obj as ByteArray)}\t"
                            )
                            sharedViewModel.sendProtocolMap[i] = data
                            Thread.sleep(50)
                        }

                    } else {
                        for (i in sharedViewModel.sendProtocolMap) {
                            if (sharedViewModel.setControlpartMap[i.key] != null) {
                                 if (sharedViewModel.setControlpartMap[i.key] == true){
                                     val msg =
                                         usbSerialHandler.obtainMessage(
                                             UsbSerialService.MSG_ROBOT_SERIAL_SEND,
                                             i.value
                                         )
                                     usbSerialHandler.sendMessage(msg)
                                     Log.d("send TEST", "${HexDump.toHexString(i.value)}")
                                 }

                            }

                        }

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

    private fun setControllerobserveThread() {
        controllerobserveThread = Thread {
            while (true) {
                try {
                    if (controllerEvent != null) {
//                        controllerPad.processJoystickInput(controllerEvent!!, -1)
//                        if (!controllerPad.isUsable) {
//                        } else {
//                            if (sharedViewModel.controlPart.value == CareRobotMC.Wheel.byte) {
//                                val tmpRPM = getRPMMath(sharedViewModel.left_Joystick.value!!)
//                                moveWheelchair(tmpRPM)
//                            } else {
//                                for (i in 0..2) {
//
//                                    stopMotor(
//                                        CareRobotMC.Left_Wheel.byte,
//                                        CareRobotMC.Right_Wheel.byte
//                                    )
//                                    Thread.sleep(20)
//                                }
//                            }
//                        }

                    }

                    Thread.sleep(80)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        controllerobserveThread?.start()
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

}