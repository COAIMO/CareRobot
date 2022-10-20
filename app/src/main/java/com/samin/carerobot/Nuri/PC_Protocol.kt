package com.samin.carerobot.Nuri

import android.util.Log
import com.jeongmin.nurimotortester.Nuri.NuriVersion
import com.jeongmin.nurimotortester.Nuri.ProtocolMode
import kotlin.experimental.inv

class PC_Protocol : ICommand {
    val TAG = "TAG"
    override var packet: Byte? = null
    override var Data: ByteArray? = null
    override var ID: Byte? = null

    override fun GetCheckSum(): Byte {
        if (Data == null) return 0
        else if (Data!!.size >= 0) {
            val sumval: UInt = Data!!.toUByteArray()
                .sum() - Data!![0].toUByte() - Data!![1].toUByte() - Data!![4].toUByte()
            return sumval.toByte().inv()
        } else return 0
    }

    // <summary>
    // 프로토콜 작성
    // </summary>
    /// <param name="id">장비 id</param>
    // <param name="size">프로토콜 사이즈</param>
    // <param name="mode">프로토콜 모드</param>
    // <param name="data">프로토콜 데이터</param>
    // <param name="isSend">시리얼 포트 전달여부 기본 : 전송</param>
    fun BuildProtocol(id: Byte, size: Byte, mode: Byte, data: ByteArray, isSend: Boolean = true) {
        val protocolSize: Int = 6 + data.size
        Data = ByteArray(protocolSize)
        Data!![0] = 0xFF.toByte()
        Data!![1] = 0xFE.toByte()
        Data!![2] = id
        Data!![3] = size
        Data!![5] = mode

        data.copyInto(Data!!, 6, 0, data.size)
        Data!![4] = GetCheckSum()
    }

    override fun Parse(data: ByteArray): Boolean {
        var ret = false
        try {
            Data = ByteArray(data.size)
            data.copyInto(Data!!, endIndex = Data!!.size)
            if (Data!![3] + 4 != data.size)
                return ret

            val chksum = GetCheckSum()
            if (Data!![4] == chksum) {
                try {
                    ID = Data!![2]
                    packet = PC_ProtocolMode.codesMap[Data!![5]]!!.byte
                    ret = true
                } catch (e: Exception) {
                    ret = false
                }
            } else ret = false

        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
        return ret
    }

    override fun GetDataStruct(): Any {
        return when (PC_ProtocolMode.codesMap[Data!![5]]) {
            PC_ProtocolMode.StopRobot -> {
                val tmp = PCtoRobotMovement()
                tmp.ID = Data!![2]
                tmp.Protocol = Data!![5]
                return tmp
            }
            PC_ProtocolMode.MoveRobot -> {
                val tmp = PCtoRobotMovement()
                tmp.ID = Data!![2]
                tmp.movement = Data!![6]
                return tmp
            }
            PC_ProtocolMode.SETSPEED -> {

            }
            PC_ProtocolMode.FEEDSPEECH -> {

            }
            else -> {}
        }

    }
}