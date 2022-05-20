package com.samin.carerobot

import android.util.Log
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.NuriPosSpeedAclCtrl
import com.samin.carerobot.Logics.CareRobotMC
import com.samin.carerobot.Logics.MotorInfo
import com.samin.carerobot.Logics.SharedViewModel
import java.util.concurrent.ConcurrentHashMap

class MotorControllerParser(viewModel: SharedViewModel) {
    val viewModel: SharedViewModel = viewModel

    // 최종 숫신시간
    val hmapLastedDate = ConcurrentHashMap<Byte, Long>()
    val exPositonhmap = HashMap<Byte, Float>()
    val motorsInfo = HashMap<Byte, MotorInfo>()
    fun parser(arg: ByteArray) {
        val encorder_id = arg[2]
        val time = System.currentTimeMillis()
        val position =
            (littleEndianConversion(arg!!.slice(7..8).toByteArray()) / 4096f * 360f)
        hmapLastedDate[encorder_id] = time
        setMotorInfo(encorder_id, position)
    }

    private fun littleEndianConversion(bytes: ByteArray): Int {
        var result = 0
        for (i in bytes.indices) {
            result = result or (bytes[i].toUByte().toInt() shl 8 * i)
        }
        return result
    }


    private fun setMotorInfo(id: Byte, position: Float) {
        val tmpInfo = MotorInfo()
        tmpInfo.encoder_id = id
        tmpInfo.position = position
        //MC id 설정
        when (id) {
            CareRobotMC.Left_Shoulder_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Left_Shoulder.byte
                tmpInfo.max_Range = 150f
                tmpInfo.min_Range = 42f
                if (tmpInfo.min_Range!! < position){
                    tmpInfo.min_Alert = true
                }else if (tmpInfo.min_Range!! > position){
                    tmpInfo.min_Alert = false
                }else if (tmpInfo.max_Range!! )
            }
            CareRobotMC.Left_Elbow_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Left_Shoulder.byte
                tmpInfo.max_Range = 350f
                tmpInfo.min_Range = 164f
            }
            CareRobotMC.Right_Shoulder_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Left_Shoulder.byte
                tmpInfo.max_Range = 210f
                tmpInfo.min_Range = 138f
            }
            CareRobotMC.Right_Elbow_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Left_Shoulder.byte
                tmpInfo.max_Range = 10f
                tmpInfo.min_Range = 196f
            }
        }


        var exPosition = exPositonhmap[id]
        if (exPosition == null) {
            exPosition = position
            exPositonhmap[id] = exPosition
        }

        if (exPosition < position) {
            tmpInfo.currnet_Direction = Direction.CW
        } else if (exPosition > position) {
            tmpInfo.currnet_Direction = Direction.CCW
        }




        viewModel.motorInfo[id] = tmpInfo
        exPositonhmap[id] = exPosition
    }
}