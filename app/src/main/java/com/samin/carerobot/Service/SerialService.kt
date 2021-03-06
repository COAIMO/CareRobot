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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.AndroidViewModel
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.NuriPosSpeedAclCtrl
import com.jeongmin.nurimotortester.Nuri.NuriProtocol
import com.jeongmin.nurimotortester.Nuri.ProtocolMode
import com.jeongmin.nurimotortester.NurirobotMC
import com.samin.carerobot.BuildConfig
import com.samin.carerobot.Logics.CareRobotMC
import com.samin.carerobot.Logics.HexDump
import com.samin.carerobot.Logics.SharedViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class SerialService : Service(), SerialInputOutputManager.Listener {
    companion object {
        const val ACTION_USB_PERMISSION_GRANTED = "USB_PERMISSION_GRANTED"
        const val ACTION_USB_PERMISSION_NOT_GRANTED = "ACTION_USB_PERMISSION_NOT_GRANTED"
        const val ACTION_USB_DEVICE_DETACHED = "ACTION_USB_DEVICE_DETACHED"
        val INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB"
        private const val BAUD_RATE = 250000
        private const val WRITE_WAIT_MILLIS = 2000
        private const val READ_WAIT_MILLIS = 2000
        var SERVICE_CONNECTED = false
        val RECEIVED_SERERIAL_DATA = 1
        const val MSG_BIND_CLIENT = 2
        const val MSG_UNBIND_CLIENT = 3
        const val MSG_SERIAL_CONNECT = 4
        const val MSG_SERIAL_SEND = 5
        const val MSG_SERIAL_RECV = 6
        const val MSG_SERIAL_DISCONNECT = 7
        const val MSG_NO_SERIAL = 8
        const val MSG_STOP_MOTOR = 9
        const val MSG_ERROR = 10
        const val MSG_SHARE_SETTING = 11

    }

    val binder = SerialServiceBinder()
    private var usbSerialPort: UsbSerialPort? = null
    var serialPortConnected = false
    lateinit var usbManager: UsbManager
    lateinit var usbDriver: UsbSerialDriver
    var usbDrivers: List<UsbSerialDriver>? = null
    var device: UsbDevice? = null
    var usbConnection: UsbDeviceConnection? = null
    private var usbIoManager: SerialInputOutputManager? = null
    private val HEADER: ByteArray = byteArrayOf(0xff.toByte(), 0xFE.toByte())
    private var lastRecvTime: Long = System.currentTimeMillis()
    private var bufferIndex: Int = 0
    private var recvBuffer: ByteArray = ByteArray(1024)
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (INTENT_ACTION_GRANT_USB.equals(intent?.action)) {
                val granted: Boolean =
                    intent?.getExtras()!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    findUSBSerialDevice(true)
                    val grantedIntent = Intent(ACTION_USB_PERMISSION_GRANTED)
                    context?.sendBroadcast(grantedIntent)
                } else {
                    val grantedIntent = Intent(ACTION_USB_PERMISSION_NOT_GRANTED)
                    context?.sendBroadcast(grantedIntent)
                }
            } else if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                if (!serialPortConnected) {
                    findUSBSerialDevice()
                }
            } else if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                val detachedIntent = Intent(ACTION_USB_DEVICE_DETACHED)
                context?.sendBroadcast(detachedIntent)
                if (!serialPortConnected) {
                    usbSerialPort?.close()
                    serialPortConnected = false
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        if (incomingHandler == null)
            incomingHandler = IncomingHandler(this)
        messenger = Messenger(incomingHandler)
        return messenger.binder
    }

    inner class SerialServiceBinder : Binder() {
        fun getService(): SerialService {
            return this@SerialService
        }
    }

    val REVC = "REVC"
    override fun onNewData(data: ByteArray?) {
        Log.d(REVC, "onNewData : ${HexDump.dumpHexString(data)}")
        if (data != null) {
            parseReceiveData(data)
        }
    }

    var mHandler = Handler()
    override fun onRunError(e: Exception?) {
        mHandler.post(Runnable {
            disconnect()
        })
    }

    override fun onCreate() {
//        GlobalScope.launch {
//            delay(5000L)
//            findUSBSerialDevice()
//            if (serialPortConnected) {
//                cancel()
//            }
//        }
        setFilter()
        super.onCreate()
    }


    override fun onDestroy() {
//        Log.d(serviceTAG, "SerialService : onDestroy")
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }


    private fun setFilter() {
        val filter = IntentFilter()
        filter.addAction(INTENT_ACTION_GRANT_USB)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(ACTION_USB_DEVICE_DETACHED)
        filter.addAction(ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(broadcastReceiver, filter)
    }

    private fun findUSBSerialDevice(hasPermission: Boolean = false) {
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
        if (usbDrivers!!.count() > 0) {
            usbDriver = usbDrivers!!.get(0)
            device = usbDriver.device

            if (!hasPermission) {
                val intent: PendingIntent =
                    PendingIntent.getBroadcast(this, 0, Intent(INTENT_ACTION_GRANT_USB), 0)
                usbManager.requestPermission(device, intent)
            } else {
                serialPortConnect()
                incomingHandler?.sendConnected()
            }
        }
    }

    private fun serialPortConnect() {
        if (usbManager.hasPermission(device) && usbSerialPort == null) {
            usbConnection = usbManager.openDevice(device)
            usbSerialPort = usbDriver.ports[0]

            usbSerialPort!!.open(usbConnection)
            usbSerialPort!!.setParameters(
                BAUD_RATE,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            usbSerialPort!!.dtr = true
            usbSerialPort!!.rts = true
            usbIoManager = SerialInputOutputManager(usbSerialPort, this)
            usbIoManager!!.readTimeout = 10
            usbIoManager!!.start()
            serialPortConnected = true
        }
    }

    private fun disconnect() {
        serialPortConnected = false
        if (usbIoManager != null) {
            usbIoManager!!.listener = null
            usbIoManager!!.stop()
        }
        usbIoManager = null
        try {
            usbSerialPort!!.close()
        } catch (ignored: IOException) {
        }
        usbSerialPort = null
    }

    fun sendData(data: ByteArray) {
        try {
            usbIoManager?.writeAsync(data)
            Log.d("??????", "send data : \n${HexDump.dumpHexString(data)}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseReceiveData(data: ByteArray) {
        lastRecvTime = System.currentTimeMillis()
        try {
            //1. ???????????????(?????? ????????? ???????????????) ??? ?????? ????????? ???????????? ????????????
            val tmpdata = ByteArray(bufferIndex + data.size)
            //2.?????? recvBurffer??? ???????????? ???????????? tmpdata??? ?????????
            System.arraycopy(recvBuffer, 0, tmpdata, 0, bufferIndex)
            //3. ???????????? tmpdata??? ?????? ????????? ?????? ????????? ??????????????? ??????
            System.arraycopy(data, 0, tmpdata, bufferIndex, data.size)
            var idx: Int = 0
//            Log.d("??????", "received = ${HexDump.dumpHexString(data)}")

            if (tmpdata.size < 6) {
                //3. ???????????? ????????? ?????? ??? ?????????????????? ????????? ??????
                System.arraycopy(tmpdata, idx, recvBuffer, 0, tmpdata.size)
                //4. ?????? ?????? ????????? ????????? ?????? ?????? ????????? ??????
                bufferIndex = tmpdata.size
                return
            }

            while (true) {
                val chkPos = indexOfBytes(tmpdata, idx, tmpdata.size)
                if (chkPos != -1) {
                    //?????? ?????? ?????? ??? ?????? ??? ?????? ????????? ??????
                    val scndpos = indexOfBytes(tmpdata, chkPos + 1, tmpdata.size)
                    //?????? ????????? ?????? ?????? -1 ??????(?????? ?????? ??????)
                    if (scndpos == -1) {
                        // ?????? ????????? ??????
                        if (tmpdata[chkPos + 3] + 4 <= tmpdata.size - chkPos) {
                            // ?????? ????????? ??? ????????? ?????? ,?????? ?????????
                            val grabageDataSize = tmpdata.size - chkPos - (tmpdata[chkPos + 3] + 4)
//                            tmpdata.lastIndex
//                            tmpdata.sliceArray(tmpdata.lastIndex-grabageDataSize..tmpdata.lastIndex)
                            //chkPos??? ?????? ???????????? ????????? ?????? ?????????????????? ??????
                            val focusdata: ByteArray =
                                tmpdata.drop(chkPos).dropLast(grabageDataSize).toByteArray()
                            recvData(focusdata)
                            bufferIndex = 0;
//                            Log.d(REVC, "parseReceiveData1 : ${HexDump.dumpHexString(focusdata)}")

                        } else {
                            //?????? ???????????? ???????????? ????????????
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

                        //????????? ?????? ????????? ?????????.(drop) //????????? ???????????? ????????? ?????? ????????? ?????????.(take)
                        val focusdata: ByteArray =
                            tmpdata.drop(chkPos).take(scndpos - chkPos).toByteArray()
                        recvData(focusdata)
                        Log.d(REVC, "parseReceiveData2 : ${HexDump.dumpHexString(focusdata)}")

                        // ????????? ?????? ????????? idx
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

    private fun indexOfBytes(data: ByteArray, startIdx: Int, count: Int): Int {
        if (data.size == 0 || count == 0 || startIdx >= count)
            return -1
        var i = startIdx
        val endIndex = Math.min(startIdx + count, data.size)
        var fidx: Int = 0
        var lastFidx = 0
        while (i < endIndex) {
            lastFidx = fidx
            fidx = if (data[i] == HEADER[fidx]) fidx + 1 else 0
            if (fidx == 2) {
                return i - fidx + 1
            }
            if (lastFidx > 0 && fidx == 0) {
                i = i - lastFidx
                lastFidx = 0
            }
            i++
        }
        return -1
    }

    val mcIDMap = hashMapOf<Byte, Byte>()

    private fun recvData(data: ByteArray) {
        val receiveParser = NurirobotMC()
        if (!receiveParser.Parse(data))
            return

        receiveParser.GetDataStruct()
        when (receiveParser.packet) {
            ProtocolMode.FEEDPing.byte -> {
                Log.d("SerialService", "CheckProductPing data : \n${HexDump.dumpHexString(data)}")
                val getID = receiveParser.Data!!.get(2)
                mcIDMap.put(getID, getID)
            }
            ProtocolMode.FEEDSpeed.byte -> {
                val motorState = receiveParser.GetDataStruct() as NuriPosSpeedAclCtrl

            }
            ProtocolMode.FEEDPos.byte -> {
                val message = Message.obtain(null, ProtocolMode.FEEDPos.byte.toInt(), data)
                incomingHandler?.sendMSG(message)
            }
        }
    }



    private lateinit var messenger: Messenger
    var incomingHandler: IncomingHandler? = null

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
                MSG_SERIAL_SEND -> {
                    val t = serialPortConnected
                    msg.data.getByteArray("")?.let { sendData(it) }
                }
                else -> super.handleMessage(msg)
            }
        }

        fun sendConnected() {
            val message = Message.obtain(null, MSG_SERIAL_CONNECT, null)
            clients.forEach {
                it.send(message)
            }
        }

        fun sendUIDATA(data: ByteArray) {
            val message = Message.obtain(null, MSG_SERIAL_RECV)
            val bundle = Bundle()
            bundle.putByteArray("", data)
            message.data = bundle
            clients.forEach {
                it.send(message)
            }
        }

        fun sendSettingDATA(data: ByteArray) {
            val message = Message.obtain(null, MSG_SHARE_SETTING)
            val bundle = Bundle()
            bundle.putByteArray("", data)
            message.data = bundle
            clients.forEach {
                it.send(message)
            }
        }

        fun sendMSG_SERIAL_DISCONNECT() {
            val message = Message.obtain(null, MSG_SERIAL_DISCONNECT)
            clients.forEach {
                it.send(message)
            }
        }

        fun sendMSG_NO_SERIAL() {
            val message = Message.obtain(null, MSG_NO_SERIAL)
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

}