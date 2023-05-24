package com.samin.carerobot.Service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.*
import android.util.Log
import android.widget.Toast
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.NuriPosSpeedAclCtrl
import com.jeongmin.nurimotortester.Nuri.ProtocolMode
import com.jeongmin.nurimotortester.NurirobotMC
import com.samin.carerobot.BuildConfig
import com.samin.carerobot.Logics.CareRobotMC
import com.samin.carerobot.Logics.HexDump
import com.samin.carerobot.MainActivity
import com.samin.carerobot.Nuri.MovementMode
import com.samin.carerobot.Nuri.PC_Protocol
import com.samin.carerobot.Nuri.PC_ProtocolMode
import com.samin.carerobot.Nuri.PCtoRobotMovement
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Runnable
import kotlin.system.measureTimeMillis

class UsbSerialService : Service() {
    companion object {
        const val ACTION_USB_PERMISSION_GRANTED_1 = "USB_PERMISSION_GRANTED_1"
        const val ACTION_USB_PERMISSION_GRANTED_2 = "USB_PERMISSION_GRANTED_2"

        const val ACTION_USB_PERMISSION_NOT_GRANTED = "ACTION_USB_PERMISSION_NOT_GRANTED"
        const val ACTION_USB_DEVICE_DETACHED = "ACTION_USB_DEVICE_DETACHED"
        private const val ACTION_USB_PERMISSION_1 = BuildConfig.APPLICATION_ID + ".GRANT_USB_1"
        private const val ACTION_USB_PERMISSION_2 = BuildConfig.APPLICATION_ID + ".GRANT_USB_2"
        private const val PORT1_BAUD_RATE = 250000
        private const val PORT2_BAUD_RATE = 1000000

        private const val WRITE_WAIT_MILLIS = 2000
        private const val READ_WAIT_MILLIS = 2000
        var SERVICE_CONNECTED = false
        val RECEIVED_SERERIAL_DATA = 1
        const val MSG_BIND_CLIENT = 2
        const val MSG_UNBIND_CLIENT = 3
        const val MSG_SERIAL_CONNECT = 4

        //        const val MSG_SERIAL_SEND = 5
//        const val MSG_SERIAL_RECV = 6
        const val MSG_SERIAL_DISCONNECT = 7
        const val MSG_NO_SERIAL = 8
        const val MSG_STOP_MOTOR = 9
        const val MSG_ERROR = 10
        const val MSG_SHARE_SETTING = 11
        const val SERIALPORT_CHK_CONNECTED = 12
        const val SERIALPORT_CONNECT = 13
        const val SERIALPORT_READY = 14
        const val MSG_STOP_ROBOT = 15
        const val MSG_MOVE_ROBOT = 16
        const val MSG_SET_SPEED = 17
        const val MSG_PC_PROTOCOL = 21

        const val MSG_ROBOT_SERIAL_SEND = 18
        const val MSG_PC_SERIAL_SEND = 19
        const val SERIALPORT_INITIALIZE = 20
    }

    private var usbSerialPort_1: UsbSerialPort? = null
    private var usbSerialPort_2: UsbSerialPort? = null
    private var robotSerial_Port: UsbSerialPort? = null
    private var pcSerial_Port: UsbSerialPort? = null

    private var receive_serialPort: UsbSerialPort? = null

    var serialPortConnected = false
    lateinit var usbManager: UsbManager
    var usbDrivers: List<UsbSerialDriver>? = null

    lateinit var usbDriver_1: UsbSerialDriver
    lateinit var usbDriver_2: UsbSerialDriver

    var device_1: UsbDevice? = null
    var device_2: UsbDevice? = null

    private var isUsbDevice_1 = false
    private var isUsbDevice_2 = false

    var usbConnection_1: UsbDeviceConnection? = null
    var usbConnection_2: UsbDeviceConnection? = null

    private var serialPort_1Connected = false
    private var serialPort_2Connected = false

    private var usbIoManager_1: SerialInputOutputManager? = null
    private var usbIoManager_2: SerialInputOutputManager? = null
    private var robotIOManager: SerialInputOutputManager? = null
    private var pcIOmanager: SerialInputOutputManager? = null


    private val port1_Listener = Port1_SerialListener()
    private val port2_Listener = Port2_SerialListener()

    private val HEADER: ByteArray = byteArrayOf(0xff.toByte(), 0xFE.toByte())
    private var lastRecvTime: Long = System.currentTimeMillis()
    private var bufferIndex: Int = 0
    private var recvBuffer: ByteArray = ByteArray(1024)


    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_USB_PERMISSION_1.equals(intent?.action)) {
                val granted: Boolean =
                    intent?.getExtras()!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    serialPort1_Connect(PORT1_BAUD_RATE)
//                    val grantedIntent = Intent(ACTION_USB_PERMISSION_GRANTED_1)
//                    context?.sendBroadcast(grantedIntent)
                    incomingHandler!!.obtainMessage(
                        SERIALPORT_CHK_CONNECTED,
                        "USB_Serial_Device1 시리얼 통신 연결되 었습니다."
                    ).sendToTarget()
                    usbDriver_2 = usbDrivers!![1]
                    device_2 = usbDriver_2.device
                    isUsbDevice_2 = true
                    if (!usbManager.hasPermission(device_2)) {
                        val intent2: PendingIntent =
                            PendingIntent.getBroadcast(
                                context,
                                0,
                                Intent(ACTION_USB_PERMISSION_2),
                                0
                            )
                        usbManager.requestPermission(device_2, intent2)
                    } else {
                        serialPort2_Connect(PORT2_BAUD_RATE)
                        incomingHandler!!.obtainMessage(
                            SERIALPORT_CHK_CONNECTED,
                            "USB_Serial_Device2 시리얼 통신 연결되 었습니다."
                        ).sendToTarget()
                        incomingHandler!!.obtainMessage(
                            SERIALPORT_CONNECT
                        ).sendToTarget()
                    }

                } else {
                    incomingHandler!!.obtainMessage(
                        SERIALPORT_CHK_CONNECTED,
                        "USB_Serial_Device1 시리얼 접근을 허용하지 않았습니다."
                    ).sendToTarget()
                }
            } else if (ACTION_USB_PERMISSION_2.equals(intent?.action)) {
                val granted: Boolean =
                    intent?.getExtras()!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    serialPort2_Connect(PORT2_BAUD_RATE)
                    incomingHandler!!.obtainMessage(
                        SERIALPORT_CHK_CONNECTED,
                        "USB_Serial_Device2 시리얼 통신 연결되 었습니다."
                    ).sendToTarget()
                    incomingHandler!!.obtainMessage(
                        SERIALPORT_CONNECT
                    ).sendToTarget()
                } else {
                    incomingHandler!!.obtainMessage(
                        SERIALPORT_CHK_CONNECTED,
                        "USB_Serial_Device2 시리얼 접근을 허용하지 않았습니다."
                    ).sendToTarget()
                }
            } else if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                Handler().postDelayed(Runnable {
                    findUSBSerialDevice()
                }, 1000)
            } else if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                disconnect()
//                mHandler.obtainMessage(
//                    SERIALPORT_CHK_CONNECTED,
//                    "USB 연결이 끊겼습니다."
//                ).sendToTarget()
            }
        }
    }

    val mcIDMap = hashMapOf<Byte, Byte>()
    private lateinit var messenger: Messenger
    var incomingHandler: IncomingHandler? = null

    override fun onBind(intent: Intent): IBinder {
        if (incomingHandler == null)
            incomingHandler = IncomingHandler(this)
        messenger = Messenger(incomingHandler)
        findUSBSerialDevice()
        return messenger.binder
    }

    override fun onCreate() {
        setFilter()
        super.onCreate()
    }


    override fun onDestroy() {
//        Log.d(serviceTAG, "SerialService : onDestroy")
        unregisterReceiver(broadcastReceiver)
        robotIOManager?.stop();
        pcIOmanager?.stop();
        usbConnection_1?.close();
        usbConnection_2?.close();

        super.onDestroy()
    }

    private fun setFilter() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION_1)
        filter.addAction(ACTION_USB_PERMISSION_2)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(broadcastReceiver, filter)
    }

    private fun findUSBSerialDevice() {
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        if (usbManager.deviceList.isEmpty()) {
            incomingHandler?.sendMSG_NO_SERIAL()
            return
        }
        usbDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (usbDrivers == null) {
            incomingHandler?.sendMSG_NO_SERIAL()
            return
        }
        if (usbDrivers?.size!! == 1) {
            incomingHandler!!.obtainMessage(
                SERIALPORT_CHK_CONNECTED,
                "USB 포트를 1개만 연결되었습니다. USB 포트를 1개 더 연결해주세요."
            ).sendToTarget()
            return
        }
        if (usbDrivers?.size!! == 2) {
//            Log.d(TAG, "connection failed: only one driver)
            incomingHandler!!.obtainMessage(
                SERIALPORT_CHK_CONNECTED,
                "USB 포트를 2개가 연결되었습니다."
            ).sendToTarget()
            usbDriver_1 = usbDrivers!![0]
            device_1 = usbDriver_1.device
            isUsbDevice_1 = true


            if (!usbManager.hasPermission(device_1)) {
                val intent: PendingIntent =
                    PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION_1), 0)
                usbManager.requestPermission(device_1, intent)
            } else {
                usbDriver_2 = usbDrivers!![1]
                device_2 = usbDriver_2.device
                isUsbDevice_2 = true
                if (!usbManager!!.hasPermission(device_2)) {
                    val intent2: PendingIntent =
                        PendingIntent.getBroadcast(
                            this,
                            0,
                            Intent(ACTION_USB_PERMISSION_2),
                            0
                        )
                    usbManager?.requestPermission(device_2, intent2)
                } else {
                    serialPortConnect(PORT1_BAUD_RATE, PORT2_BAUD_RATE)
                    incomingHandler!!.obtainMessage(
                        SERIALPORT_CONNECT
                    ).sendToTarget()
                }
            }
        }
    }

    private fun serialPortConnect(port_1_Buadrate: Int?, port_2_Buadrate: Int?) {
        if (usbManager != null) {
            if (usbManager.hasPermission(device_1) && usbSerialPort_1 == null) {
                try {
                    usbConnection_1 = usbManager.openDevice(device_1)
                    usbSerialPort_1 = usbDriver_1.ports[0]
                    usbSerialPort_1!!.open(usbConnection_1)
                    usbSerialPort_1!!.setParameters(
                        port_1_Buadrate!!,
                        UsbSerialPort.DATABITS_8,
                        UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE
                    )
                    usbSerialPort_1!!.dtr = true
                    usbSerialPort_1!!.rts = true
                    usbIoManager_1 = SerialInputOutputManager(usbSerialPort_1, port1_Listener)
                    usbIoManager_1!!.readTimeout = 10
                    usbIoManager_1!!.start()
                    serialPort_1Connected = true
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                    val message = Message.obtain(null, MSG_ERROR)
                    incomingHandler?.sendMSG(message)
                    return
                }

//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT1_CHK_CONNECTED
//                    arg1 = 1
//                    obj = baud_rate
//                }
//                mHandler.sendMessage(msg)
            } else {
//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT1_CHK_CONNECTED
//                    arg1 = 2
//                }
//                mHandler.sendMessage(msg)
            }

            if (usbManager!!.hasPermission(device_2) && usbSerialPort_2 == null) {
                try {
                    usbConnection_2 = usbManager!!.openDevice(device_2)
                    usbSerialPort_2 = usbDriver_2!!.ports[0]
                    usbSerialPort_2!!.open(usbConnection_2)
                    usbSerialPort_2!!.setParameters(
                        port_2_Buadrate!!,
                        UsbSerialPort.DATABITS_8,
                        UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE
                    )
                    usbSerialPort_2!!.dtr = true
                    usbSerialPort_2!!.rts = true
                    usbIoManager_2 = SerialInputOutputManager(usbSerialPort_2, port2_Listener)
                    usbIoManager_2!!.readTimeout = 10
                    usbIoManager_2!!.start()
                    serialPort_2Connected = true
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                    val message = Message.obtain(null, MSG_ERROR)
                    incomingHandler?.sendMSG(message)
                    return
                }

//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT2_CHK_CONNECTED
//                    arg1 = 1
//                    obj = baud_rate
//                }
//                mHandler.sendMessage(msg)
            } else {
//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT2_CHK_CONNECTED
//                    arg1 = 2
//                }
//                mHandler.sendMessage(msg)
            }
        } else if (usbManager == null) {
//            mHandler.obtainMessage(SERIALPORT_CHK_CONNECTED, "시리얼 포트를 연결해 주세요.").sendToTarget()
        }
    }

    private fun serialPort1_Connect(buadrate: Int) {
        if (usbManager.hasPermission(device_1) && usbSerialPort_1 == null) {
            usbConnection_1 = usbManager.openDevice(device_1)
            usbSerialPort_1 = usbDriver_1.ports[0]
            usbSerialPort_1!!.open(usbConnection_1)
            usbSerialPort_1!!.setParameters(
                buadrate!!,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            usbSerialPort_1!!.dtr = true
            usbSerialPort_1!!.rts = true
            usbIoManager_1 = SerialInputOutputManager(usbSerialPort_1, port1_Listener)
            usbIoManager_1!!.readTimeout = 0
            usbIoManager_1!!.start()
            serialPort_1Connected = true
//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT1_CHK_CONNECTED
//                    arg1 = 1
//                    obj = baud_rate
//                }
//                mHandler.sendMessage(msg)
        } else {
//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT1_CHK_CONNECTED
//                    arg1 = 2
//                }
//                mHandler.sendMessage(msg)
        }
    }

    private fun serialPort2_Connect(buadrate: Int) {
        if (usbManager.hasPermission(device_2) && usbSerialPort_2 == null) {
            usbConnection_2 = usbManager.openDevice(device_2)
            usbSerialPort_2 = usbDriver_2.ports[0]
            usbSerialPort_2!!.open(usbConnection_2)
            usbSerialPort_2!!.setParameters(
                buadrate,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            usbSerialPort_2!!.dtr = true
            usbSerialPort_2!!.rts = true
            usbIoManager_2 = SerialInputOutputManager(usbSerialPort_2, port2_Listener)
            usbIoManager_2!!.readTimeout = 0
            usbIoManager_2!!.start()
            serialPort_2Connected = true
//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT1_CHK_CONNECTED
//                    arg1 = 1
//                    obj = baud_rate
//                }
//                mHandler.sendMessage(msg)
        } else {
//                val msg: Message = mHandler.obtainMessage().apply {
//                    what = PORT1_CHK_CONNECTED
//                    arg1 = 2
//                }
//                mHandler.sendMessage(msg)
        }
    }

    private fun disconnect() {
        isFirst = true
        serialPort_1Connected = false
        serialPort_2Connected = false
        usbIoManager_1?.stop()
        usbIoManager_2?.stop()
        usbIoManager_1 = null
        usbIoManager_2 = null
        try {
            usbSerialPort_1?.close()
            usbSerialPort_2?.close()

            usbConnection_1?.close()
            usbConnection_2?.close()
        } catch (ignored: IOException) {
            ignored.printStackTrace();
        }
        usbDrivers = null
        usbSerialPort_1 = null
        usbSerialPort_2 = null
    }

    fun port1_SendData(data: ByteArray) {
        try {
            usbIoManager_1?.writeAsync(data)
//            Log.d("로그", "port1_SendData : \n${HexDump.dumpHexString(data)}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun port2_SendData(data: ByteArray) {
        try {
            usbIoManager_2?.writeAsync(data)
//            Log.d("로그", "port2_SendData : \n${HexDump.dumpHexString(data)}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun robot_SendData(data: ByteArray) {
        try {
            robotIOManager?.writeAsync(data)
//            Log.d("로그", "port1_SendData : \n${HexDump.dumpHexString(data)}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pc_SendData(data: ByteArray) {
        try {
            pcIOmanager?.writeAsync(data)
//            Log.d("로그", "port2_SendData : \n${HexDump.dumpHexString(data)}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseReceiveRobotData(data: ByteArray) {
        lastRecvTime = System.currentTimeMillis()
        try {
            //1. 버퍼인덱스(이전 부족한 데이터크기) 및 받은 데이터 크기만큼 배열생성
            val tmpdata = ByteArray(bufferIndex + data.size)
            //2.이전 recvBurffer에 남아있는 데이터를 tmpdata로 이동함
            System.arraycopy(recvBuffer, 0, tmpdata, 0, bufferIndex)
            //3. 데이터를 tmpdata의 잔여 데이터 뒤에 데이터 사이즈만큼 넣음
            System.arraycopy(data, 0, tmpdata, bufferIndex, data.size)
            var idx: Int = 0
//            Log.d("태그", "received = ${HexDump.dumpHexString(data)}")

            if (tmpdata.size < 6) {
                //3. 수신받은 데이터 부족 시 리시브버퍼로 데이터 이동
                System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                //4. 이전 받은 데이터 확인을 위해 버퍼 인덱스 수정
                bufferIndex = tmpdata.size
                return
            }

            while (true) {
                val chkPos = indexOfBytes(tmpdata, idx, tmpdata.size)
                if (chkPos != -1) {
                    //해더 유무 체크 및 헤더 몇 번째 있는지 반환
                    val scndpos = indexOfBytes(tmpdata, chkPos + 1, tmpdata.size)
                    //다음 헤더가 없는 경우 -1 변환(헤더 중복 체크)
                    if (scndpos == -1) {
                        // 다음 데이터 없음
                        if (chkPos + 3 < tmpdata.size && tmpdata[chkPos + 3] + 4 <= tmpdata.size - chkPos) {
                            // 해당 전문을 다 받았을 경우 ,또는 크거나
                            val grabageDataSize = tmpdata.size - chkPos - (tmpdata[chkPos + 3] + 4)
//                            tmpdata.lastIndex
//                            tmpdata.sliceArray(tmpdata.lastIndex-grabageDataSize..tmpdata.lastIndex)
                            //chkPos로 헤더 앞데이터 자르고 뒤에 가바지데이터 제거
                            val focusdata: ByteArray =
                                tmpdata.drop(chkPos).dropLast(grabageDataSize).toByteArray()
                            recvRobotData(focusdata)
                            bufferIndex = 0;

                        } else {
                            //해당 전문보다 데이터가 작을경우
                            System.arraycopy(
                                tmpdata,
                                chkPos,
                                recvBuffer,
                                0,
                                tmpdata.size - chkPos
                            )
                            bufferIndex = tmpdata.size - chkPos
                        }
                        break

                    } else {

                        //첫번째 헤더 앞부분 짤라냄.(drop) //첫번째 헤더부터 두번째 헤더 앞까지 짤라냄.(take)
                        val focusdata: ByteArray =
                            tmpdata.drop(chkPos).take(scndpos - chkPos).toByteArray()
                        recvRobotData(focusdata)
                        // 두번째 헤더 부분을 idx
                        idx = scndpos
                    }
                } else {
                    System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                    bufferIndex = tmpdata.size
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseReceivePCData(data: ByteArray) {
        lastRecvTime = System.currentTimeMillis()
        try {
            //1. 버퍼인덱스(이전 부족한 데이터크기) 및 받은 데이터 크기만큼 배열생성
            val tmpdata = ByteArray(bufferIndex + data.size)
            //2.이전 recvBurffer에 남아있는 데이터를 tmpdata로 이동함
            System.arraycopy(recvBuffer, 0, tmpdata, 0, bufferIndex)
            //3. 데이터를 tmpdata의 잔여 데이터 뒤에 데이터 사이즈만큼 넣음
            System.arraycopy(data, 0, tmpdata, bufferIndex, data.size)
            var idx: Int = 0
//            Log.d("태그", "received = ${HexDump.dumpHexString(data)}")

            if (tmpdata.size < 6) {
                //3. 수신받은 데이터 부족 시 리시브버퍼로 데이터 이동
                System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                //4. 이전 받은 데이터 확인을 위해 버퍼 인덱스 수정
                bufferIndex = tmpdata.size
                return
            }

            while (true) {
                val chkPos = indexOfBytes(tmpdata, idx, tmpdata.size)
                if (chkPos != -1) {
                    //해더 유무 체크 및 헤더 몇 번째 있는지 반환
                    val scndpos = indexOfBytes(tmpdata, chkPos + 1, tmpdata.size)
                    //다음 헤더가 없는 경우 -1 변환(헤더 중복 체크)
                    if (scndpos == -1) {
                        // 다음 데이터 없음
                        if (tmpdata[chkPos + 3] + 4 <= tmpdata.size - chkPos) {
                            // 해당 전문을 다 받았을 경우 ,또는 크거나
                            val grabageDataSize = tmpdata.size - chkPos - (tmpdata[chkPos + 3] + 4)
//                            tmpdata.lastIndex
//                            tmpdata.sliceArray(tmpdata.lastIndex-grabageDataSize..tmpdata.lastIndex)
                            //chkPos로 헤더 앞데이터 자르고 뒤에 가바지데이터 제거
                            val focusdata: ByteArray =
                                tmpdata.drop(chkPos).dropLast(grabageDataSize).toByteArray()
                            recvPCData(focusdata)
                            bufferIndex = 0;

                        } else {
                            //해당 전문보다 데이터가 작을경우
                            System.arraycopy(
                                tmpdata,
                                chkPos,
                                recvBuffer,
                                0,
                                tmpdata.size - chkPos
                            )
                            bufferIndex = tmpdata.size - chkPos
                        }
                        break

                    } else {

                        //첫번째 헤더 앞부분 짤라냄.(drop) //첫번째 헤더부터 두번째 헤더 앞까지 짤라냄.(take)
                        val focusdata: ByteArray =
                            tmpdata.drop(chkPos).take(scndpos - chkPos).toByteArray()
                        recvPCData(focusdata)
                        // 두번째 헤더 부분을 idx
                        idx = scndpos
                    }
                } else {
                    System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                    bufferIndex = tmpdata.size
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


//    private fun indexOfBytes(data: ByteArray, startIdx: Int, count: Int): Int {
//        if (data.size == 0 || count == 0 || startIdx >= count)
//            return -1
//        var i = startIdx
//        val endIndex = Math.min(startIdx + count, data.size)
//        var fidx: Int = 0
//        var lastFidx = 0
//        while (i < endIndex) {
//            lastFidx = fidx
//            fidx = if (data[i] == HEADER[fidx]) fidx + 1 else 0
//            if (fidx == 2) {
//                return i - fidx + 1
//            }
//            if (lastFidx > 0 && fidx == 0) {
//                i = i - lastFidx
//                lastFidx = 0
//            }
//            i++
//        }
//        return -1
//    }

    private fun indexOfBytes(data: ByteArray, startIdx: Int, count: Int): Int {
        if (data.size == 0 || count == 0 || startIdx >= count)
            return -1
        var i = startIdx
        val endIndex = Math.min(startIdx + count, data.size)
        var fidx: Int = 0
        while (i < endIndex) {
            if (data[i] == HEADER[fidx]) {
                fidx++
                if (fidx == 2) {
                    return i - fidx + 1
                }
            } else {
                fidx = 0
            }
            i++
        }
        return -1
    }

    private fun recvRobotData(data: ByteArray) {
        val receiveParser = NurirobotMC()
        if (!receiveParser.Parse(data))
            return

        receiveParser.GetDataStruct()
        when (receiveParser.packet) {
            ProtocolMode.FEEDPing.byte -> {
//                Log.d("ssf", "CheckProductPing data : \n${HexDump.dumpHexString(data)}")
                val getID = receiveParser.Data!!.get(2)
                mcIDMap.put(getID, getID)
            }
            ProtocolMode.FEEDSpeed.byte -> {
                val motorState = receiveParser.GetDataStruct() as NuriPosSpeedAclCtrl

            }
            ProtocolMode.FEEDPos.byte -> {
                val message = Message.obtain(null, ProtocolMode.FEEDPos.byte.toInt(), data)
                Log.d("위치 피드백", "${HexDump.dumpHexString(data)}")
                incomingHandler?.sendMSG(message)
            }
            ProtocolMode.FEEDFirmware.byte -> {
                Log.d("ssf", "CheckProductPing data : \n${HexDump.dumpHexString(data)}")
            }
        }
    }

    private fun recvPCData(data: ByteArray) {
        val receiveParser = PC_Protocol()
        if (!receiveParser.Parse(data))
            return
        if (receiveParser.ID == 1.toByte())
            when (receiveParser.packet) {
                PC_ProtocolMode.StopRobot.byte -> {
                    val message = Message.obtain(null, MSG_STOP_ROBOT, data)
                    incomingHandler?.sendMSG(message)
                    val msg = Message.obtain(null, MSG_PC_PROTOCOL, receiveParser.Data!!)
                    incomingHandler?.sendMSG(msg)
                }
                PC_ProtocolMode.MoveRobot.byte -> {
                    val message = Message.obtain(null, MSG_MOVE_ROBOT, receiveParser.Data!![6])
                    incomingHandler?.sendMSG(message)
                    val msg = Message.obtain(null, MSG_PC_PROTOCOL, receiveParser.Data!!)
                    incomingHandler?.sendMSG(msg)
                }
                PC_ProtocolMode.SETSPEED.byte -> {
                    val message = Message.obtain(null, MSG_SET_SPEED, receiveParser.Data!![6])
                    incomingHandler?.sendMSG(message)
                    val msg = Message.obtain(null, MSG_PC_PROTOCOL, receiveParser.Data!!)
                    incomingHandler?.sendMSG(msg)
                }
                PC_ProtocolMode.FEEDSPEECH.byte -> {

                }
            }
    }


    val feedbackallList = listOf<Byte>(
        CareRobotMC.Left_Shoulder_Encoder.byte,
        CareRobotMC.Right_Shoulder_Encoder.byte,
        CareRobotMC.Waist.byte,
        CareRobotMC.Waist_Sensor.byte
    )

    lateinit var checkPortThread: Thread
    private fun checkPort() {
        incomingHandler!!.obtainMessage(
            SERIALPORT_INITIALIZE
        ).sendToTarget()

        val sendParser = NurirobotMC()
        val startTime = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            async {
                while (true) {
                    if (isFirst){
                        val curr = System.currentTimeMillis()
                        for (i in feedbackallList) {
                            sendParser.Feedback(i, ProtocolMode.REQPing.byte)
                            val data = sendParser.Data!!.clone()
                            port1_SendData(data)
                            delay(100)
                        }

                        if (curr > startTime + 3000) {
                            disconnect()
                            delay(500)
                            serialPortConnect(PORT2_BAUD_RATE, PORT1_BAUD_RATE)
                            delay(500)
                            break
                        }
                    }else break
                }
            }.await()
            async {
                val startTime_2 = System.currentTimeMillis()

                while (true) {
                    if (isFirst){
                        val curr = System.currentTimeMillis()
                        for (i in feedbackallList) {
                            sendParser.Feedback(i, ProtocolMode.REQPing.byte)
                            val data = sendParser.Data!!.clone()
                            port2_SendData(data)
                            delay(100)
                        }

                        if (curr > startTime_2 + 3000) {
                            val message = Message.obtain(null, MSG_ERROR)
                            incomingHandler?.sendMSG(message)
                            break
                        }
                    }else break
                }
            }
        }

    }

    inner class IncomingHandler(
        service: Service,
        private val context: Context = service.applicationContext
    ) :
        Handler(Looper.getMainLooper()) {

        private val clients = mutableListOf<Messenger>()

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_BIND_CLIENT -> {
                    clients.add(msg.replyTo)
                    findUSBSerialDevice()
                }
                MSG_UNBIND_CLIENT -> clients.remove(msg.replyTo)
//                MSG_SERIAL_SEND -> {
//                    val t = serialPortConnected
//                    msg.data.getByteArray("")?.let { sendData(it) }
//                }
//                SERIALPORT_CHK_CONNECTED -> {
//                    Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show()
//                }

                SERIALPORT_CONNECT -> checkPort()
                SERIALPORT_READY -> {
                    isFirst = false
                    sendSerialReady()
                }

                MSG_PC_SERIAL_SEND -> {
                    msg.data.getByteArray("").let {
                        pc_SendData(it!!)
                    }
                }
                MSG_ROBOT_SERIAL_SEND -> {
                    msg.data.getByteArray("").let {
                        robot_SendData(it!!)
                    }
                }
                SERIALPORT_INITIALIZE->{
                    val message = Message.obtain(null, SERIALPORT_INITIALIZE, null)
                    clients.forEach {
                        it.send(message)
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
        fun sendSerialReady(){
            val message = Message.obtain(null, SERIALPORT_READY, null)
            clients.forEach {
                it.send(message)
            }
        }

        fun sendConnected() {
            val message = Message.obtain(null, SerialService.MSG_SERIAL_CONNECT, null)
            clients.forEach {
                it.send(message)
            }
        }

        fun sendUIDATA(data: ByteArray) {
            val message = Message.obtain(null, SerialService.MSG_SERIAL_RECV)
            val bundle = Bundle()
            bundle.putByteArray("", data)
            message.data = bundle
            clients.forEach {
                it.send(message)
            }
        }

        fun sendSettingDATA(data: ByteArray) {
            val message = Message.obtain(null, SerialService.MSG_SHARE_SETTING)
            val bundle = Bundle()
            bundle.putByteArray("", data)
            message.data = bundle
            clients.forEach {
                it.send(message)
            }
        }

        fun sendMSG_SERIAL_DISCONNECT() {
            val message = Message.obtain(null, SerialService.MSG_SERIAL_DISCONNECT)
            clients.forEach {
                it.send(message)
            }
        }

        fun sendMSG_NO_SERIAL() {
            val message = Message.obtain(null, SerialService.MSG_NO_SERIAL)
            clients.forEach {
                it.send(message)
            }
        }

        fun sendMSG(msg: Message) {
            clients.forEach {
                it.send(msg)
            }
        }
    }

    var isFirst = true

    inner class Port1_SerialListener() : SerialInputOutputManager.Listener {
        private lateinit var ioManager: SerialInputOutputManager
        override fun onNewData(data: ByteArray?) {
            try {
                if (data != null) {
                    if (isFirst) {
                        parseCheckData(data)
                    } else {
                        if (usbIoManager_1 == robotIOManager) {
                            parseReceiveRobotData(data)
                        } else {
                            parseReceivePCData(data)
                        }
                    }
                    Log.d("TEST", "port1_recv : ${HexDump.dumpHexString(data)}")
                }
            } catch (e: Exception) {
            }
        }

        override fun onRunError(e: java.lang.Exception?) {
            try {
                e?.printStackTrace()
            } catch (e: Exception) {
            }
        }

//        private fun parseCheckData(data: ByteArray) {
//            lastRecvTime = System.currentTimeMillis()
//            try {
//                //1. 버퍼인덱스(이전 부족한 데이터크기) 및 받은 데이터 크기만큼 배열생성
//                val tmpdata = ByteArray(bufferIndex + data.size)
//                //2.이전 recvBurffer에 남아있는 데이터를 tmpdata로 이동함
//                System.arraycopy(recvBuffer, 0, tmpdata, 0, bufferIndex)
//                //3. 데이터를 tmpdata의 잔여 데이터 뒤에 데이터 사이즈만큼 넣음
//                System.arraycopy(data, 0, tmpdata, bufferIndex, data.size)
//                var idx: Int = 0
////            Log.d("태그", "received = ${HexDump.dumpHexString(data)}")
//
//                if (tmpdata.size < 6) {
//                    //3. 수신받은 데이터 부족 시 리시브버퍼로 데이터 이동
//                    System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
//                    //4. 이전 받은 데이터 확인을 위해 버퍼 인덱스 수정
//                    bufferIndex = tmpdata.size
//                    return
//                }
//
//                while (true) {
//                    val chkPos = indexOfBytes(tmpdata, idx, tmpdata.size)
//                    if (chkPos != -1) {
//                        //해더 유무 체크 및 헤더 몇 번째 있는지 반환
//                        val scndpos = indexOfBytes(tmpdata, chkPos + 1, tmpdata.size)
//                        //다음 헤더가 없는 경우 -1 변환(헤더 중복 체크)
//                        if (scndpos == -1) {
//                            // 다음 데이터 없음
//                            if (tmpdata[chkPos + 3] + 4 <= tmpdata.size - chkPos) {
//                                // 해당 전문을 다 받았을 경우 ,또는 크거나
//                                val grabageDataSize =
//                                    tmpdata.size - chkPos - (tmpdata[chkPos + 3] + 4)
////                            tmpdata.lastIndex
////                            tmpdata.sliceArray(tmpdata.lastIndex-grabageDataSize..tmpdata.lastIndex)
//                                //chkPos로 헤더 앞데이터 자르고 뒤에 가바지데이터 제거
//                                val focusdata: ByteArray =
//                                    tmpdata.drop(chkPos).dropLast(grabageDataSize).toByteArray()
//                                checkData(focusdata)
//                                bufferIndex = 0;
//
//                            } else {
//                                //해당 전문보다 데이터가 작을경우
//                                System.arraycopy(
//                                    tmpdata,
//                                    chkPos,
//                                    recvBuffer,
//                                    0,
//                                    tmpdata.size - chkPos
//                                )
//                                bufferIndex = tmpdata.size - chkPos
//                            }
//                            break
//
//                        } else {
//
//                            //첫번째 헤더 앞부분 짤라냄.(drop) //첫번째 헤더부터 두번째 헤더 앞까지 짤라냄.(take)
//                            val focusdata: ByteArray =
//                                tmpdata.drop(chkPos).take(scndpos - chkPos).toByteArray()
//                            checkData(focusdata)
//                            // 두번째 헤더 부분을 idx
//                            idx = scndpos
//                        }
//                    } else {
//                        System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
//                        bufferIndex = tmpdata.size
//                        break
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
        private fun parseCheckData(data: ByteArray) {
            val elapsed: Long = measureTimeMillis {
                lastRecvTime = System.currentTimeMillis()
                try {
                    val tmpdata = ByteArray(bufferIndex + data.size)
                    System.arraycopy(recvBuffer, 0, tmpdata, 0, bufferIndex)
                    System.arraycopy(data, 0, tmpdata, bufferIndex, data.size)
                    var idx: Int = 0

                    if (bufferIndex == 0 && tmpdata.size < 6) {
                        System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                        bufferIndex = tmpdata.size
                        return
                    }

                    while (true) {
                        val chkPos = indexOfBytes(tmpdata, idx, tmpdata.size)
                        if (chkPos == -1) {
                            System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                            bufferIndex = tmpdata.size
                            break
                        }

                        val scndpos = indexOfBytes(tmpdata, chkPos + 1, tmpdata.size)
                        if (scndpos == -1) {
                            if (chkPos + 3 < tmpdata.size && tmpdata[chkPos + 3] + 4 <= tmpdata.size - chkPos) {
                                val focusdata = tmpdata.copyOfRange(chkPos, chkPos + tmpdata[chkPos + 3] + 4)
//                                Log.d("usbserialservice", "parseReceiveData1 : ${HexDump.dumpHexString(focusdata)}")
                                checkData(focusdata)
                                bufferIndex = 0;
                            } else {
                                System.arraycopy(
                                    tmpdata,
                                    chkPos,
                                    recvBuffer,
                                    0,
                                    tmpdata.size - chkPos
                                )
                                bufferIndex = tmpdata.size - chkPos
                            }
                            break

                        } else {
                            if (tmpdata[chkPos + 3] + 4 <= scndpos) {
                                val focusdata = tmpdata.copyOfRange(chkPos, scndpos)
//                                Log.d("usbserialservice", "parseReceiveData2 : ${HexDump.dumpHexString(focusdata)}")
                                checkData(focusdata)
                            }
                            idx = scndpos
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            Log.d("usbserialservice", "calc time  : ${elapsed}")
        }

        private fun checkData(data: ByteArray) {
            val receiveParser = NurirobotMC()
            if (!receiveParser.Parse(data))
                return

            receiveParser.GetDataStruct()
            if (receiveParser.packet == ProtocolMode.FEEDPing.byte) {
                robotSerial_Port = usbSerialPort_1
                robotIOManager = usbIoManager_1
                pcSerial_Port = usbSerialPort_2
                pcIOmanager = usbIoManager_2

                incomingHandler!!.obtainMessage(
                    SERIALPORT_READY
                ).sendToTarget()
            }
        }
    }

    inner class Port2_SerialListener() : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray?) {
            try {
                if (data != null) {
                    if (isFirst) {
                        parseCheckData(data)
                    } else {
                        if (usbIoManager_2 == robotIOManager) {
                            parseReceiveRobotData(data)
                        } else {
                            parseReceivePCData(data)
                        }
                    }
//                    Log.d("TEST", "port2_recv : ${HexDump.dumpHexString(data)}")
                }
            } catch (e: Exception) {
            }
        }

        override fun onRunError(e: java.lang.Exception?) {
            try {
                e?.printStackTrace()
            } catch (e: Exception) {
            }
        }

        private fun parseCheckData(data: ByteArray) {
            lastRecvTime = System.currentTimeMillis()
            try {
                //1. 버퍼인덱스(이전 부족한 데이터크기) 및 받은 데이터 크기만큼 배열생성
                val tmpdata = ByteArray(bufferIndex + data.size)
                //2.이전 recvBurffer에 남아있는 데이터를 tmpdata로 이동함
                System.arraycopy(recvBuffer, 0, tmpdata, 0, bufferIndex)
                //3. 데이터를 tmpdata의 잔여 데이터 뒤에 데이터 사이즈만큼 넣음
                System.arraycopy(data, 0, tmpdata, bufferIndex, data.size)
                var idx: Int = 0
//            Log.d("태그", "received = ${HexDump.dumpHexString(data)}")

                if (tmpdata.size < 6) {
                    //3. 수신받은 데이터 부족 시 리시브버퍼로 데이터 이동
                    System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                    //4. 이전 받은 데이터 확인을 위해 버퍼 인덱스 수정
                    bufferIndex = tmpdata.size
                    return
                }

                while (true) {
                    val chkPos = indexOfBytes(tmpdata, idx, tmpdata.size)
                    if (chkPos != -1) {
                        //해더 유무 체크 및 헤더 몇 번째 있는지 반환
                        val scndpos = indexOfBytes(tmpdata, chkPos + 1, tmpdata.size)
                        //다음 헤더가 없는 경우 -1 변환(헤더 중복 체크)
                        if (scndpos == -1) {
                            // 다음 데이터 없음
                            if (tmpdata[chkPos + 3] + 4 <= tmpdata.size - chkPos) {
                                // 해당 전문을 다 받았을 경우 ,또는 크거나
                                val grabageDataSize =
                                    tmpdata.size - chkPos - (tmpdata[chkPos + 3] + 4)
//                            tmpdata.lastIndex
//                            tmpdata.sliceArray(tmpdata.lastIndex-grabageDataSize..tmpdata.lastIndex)
                                //chkPos로 헤더 앞데이터 자르고 뒤에 가바지데이터 제거
                                val focusdata: ByteArray =
                                    tmpdata.drop(chkPos).dropLast(grabageDataSize).toByteArray()
                                checkData(focusdata)
                                bufferIndex = 0;

                            } else {
                                //해당 전문보다 데이터가 작을경우
                                System.arraycopy(
                                    tmpdata,
                                    chkPos,
                                    recvBuffer,
                                    0,
                                    tmpdata.size - chkPos
                                )
                                bufferIndex = tmpdata.size - chkPos
                            }
                            break

                        } else {

                            //첫번째 헤더 앞부분 짤라냄.(drop) //첫번째 헤더부터 두번째 헤더 앞까지 짤라냄.(take)
                            val focusdata: ByteArray =
                                tmpdata.drop(chkPos).take(scndpos - chkPos).toByteArray()
                            checkData(focusdata)
                            // 두번째 헤더 부분을 idx
                            idx = scndpos
                        }
                    } else {
                        System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                        bufferIndex = tmpdata.size
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun checkData(data: ByteArray) {
            val receiveParser = NurirobotMC()
            if (!receiveParser.Parse(data))
                return

            receiveParser.GetDataStruct()
            if (receiveParser.packet == ProtocolMode.FEEDPing.byte) {
                robotSerial_Port = usbSerialPort_2
                robotIOManager = usbIoManager_2
                pcSerial_Port = usbSerialPort_1
                pcIOmanager = usbIoManager_1
                incomingHandler!!.obtainMessage(
                    SERIALPORT_READY
                ).sendToTarget()
            }
        }
    }

}