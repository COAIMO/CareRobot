package com.samin.carerobot

import android.util.Log
import com.jeongmin.nurimotortester.Nuri.Direction
import com.samin.carerobot.Logics.CareRobotMC
import com.samin.carerobot.Logics.MotorInfo
import com.samin.carerobot.Logics.SharedViewModel
import java.lang.Math.abs
import java.util.concurrent.ConcurrentHashMap

class MotorControllerParser(viewModel: SharedViewModel) {
    val viewModel: SharedViewModel = viewModel

    // 최종 숫신시간
    val hmapLastedDate = ConcurrentHashMap<Byte, Long>()
    val exPositonhmap = HashMap<Byte, Float>()
    val exDirection = HashMap<Byte, Direction>()
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

        var exPosition = exPositonhmap[id]
        if (exPosition == null) {
            exPosition = position
            exPositonhmap[id] = exPosition
        }

        //MC id 설정
        when (id) {
            CareRobotMC.Left_Shoulder_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Left_Shoulder.byte
                tmpInfo.max_Range = 150f
                tmpInfo.min_Range = 42f
                if (exPositonhmap[id]!! < position) {
                    tmpInfo.currnet_Direction = Direction.CCW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else if (exPositonhmap[id]!! > position) {
                    tmpInfo.currnet_Direction = Direction.CW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else {
                    tmpInfo.currnet_Direction = exDirection[id]
                }

                // 에러 범위 안인가?
                if (tmpInfo.position!! in tmpInfo.min_Range!!..tmpInfo.max_Range!!) {
                    val v1 = tmpInfo.min_Range!! - tmpInfo.position!!
                    val v2 = tmpInfo.max_Range!! - tmpInfo.position!!
                    // min 또는 max에 가깝게 있는가?
                    if (abs(v1) < abs(v2)) {
                        //min이랑 가까울때
                        if (viewModel.controlDirection != Direction.CW) {
                            tmpInfo.min_Alert = true
                        }
                    } else {
                        //max랑 가까울때
                        if (viewModel.controlDirection != Direction.CCW) {
                            tmpInfo.max_Alert = true
                        }
                    }
                } else {
                    // 정상 범위일때
//                    if (viewModel.controlDirection == Direction.CW) {
////                        //시계방향일때는 max값이 랑만 비교
////                        if (tmpInfo.position!! < tmpInfo.max_Range!! && tmpInfo.position!! + 360 < tmpInfo.max_Range!!)
////                            //42f ~ 0f 있을때 현재위치가 max값 무조건 작기때문에, 360더한 값이 max 값보다 작으면 비정상범위에 있음.
////                            tmpInfo.max_Alert = true
////                        else if (tmpInfo.position!! > tmpInfo.max_Range!! && tmpInfo.position!! < tmpInfo.max_Range!!)
////                            tmpInfo.max_Alert = true
//
//
//                        if (tmpInfo.position!! + 360 < tmpInfo.max_Range!!) {
//                            tmpInfo.max_Alert = true
//                        }
//
//
//                    } else if (viewModel.controlDirection == Direction.CCW) {
////                        if (tmpInfo.position!! > tmpInfo.min_Range!! && tmpInfo.position!! > tmpInfo.min_Range!! + 360)
////                            tmpInfo.min_Alert = true
////                        else if (tmpInfo.position!! < tmpInfo.min_Range!! && tmpInfo.position!! > tmpInfo.min_Range!!)
////                            tmpInfo.min_Alert = true
//                        if (tmpInfo.position!! > tmpInfo.min_Range!! + 360)
//                            tmpInfo.min_Alert = true
//                    }
                }
            }
            CareRobotMC.Left_Elbow_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Left_Elbow.byte
                tmpInfo.max_Range = 345f
                tmpInfo.min_Range = 170f
                if (exPositonhmap[id]!! < position) {
                    tmpInfo.currnet_Direction = Direction.CCW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else if (exPositonhmap[id]!! > position) {
                    tmpInfo.currnet_Direction = Direction.CW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else {
                    tmpInfo.currnet_Direction = exDirection[id]
                }

                // 에러 범위 안인가?
                if (tmpInfo.position!! < tmpInfo.min_Range!!) {
                    if (viewModel.controlDirection != Direction.CCW) {
                        tmpInfo.min_Alert = true
                    }
                } else if (tmpInfo.position!! > tmpInfo.max_Range!!) {
                    if (viewModel.controlDirection != Direction.CW) {
                        tmpInfo.max_Alert = true
                    }
                }

            }
            CareRobotMC.Right_Shoulder_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Right_Shoulder.byte
                tmpInfo.min_Range = 210f
                tmpInfo.max_Range = 318f
                if (exPositonhmap[id]!! < position) {
                    tmpInfo.currnet_Direction = Direction.CCW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else if (exPositonhmap[id]!! > position) {
                    tmpInfo.currnet_Direction = Direction.CW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else {
                    tmpInfo.currnet_Direction = exDirection[id]
                }
                // 에러 범위 안인가?
                if (tmpInfo.position!! in tmpInfo.min_Range!!..tmpInfo.max_Range!!) {
                    val min = tmpInfo.min_Range!! - tmpInfo.position!!
                    val max = tmpInfo.max_Range!! - tmpInfo.position!!
                    // min 또는 max에 가깝게 있는가?
                    if (abs(min) < abs(max)) {
                        //min이랑 가까울때
                        if (viewModel.controlDirection != Direction.CW) {
                            tmpInfo.min_Alert = true
                        }
                    } else {
                        //max랑 가까울때
                        if (viewModel.controlDirection != Direction.CCW) {
                            tmpInfo.max_Alert = true
                        }
                    }
                }
            }
            CareRobotMC.Right_Elbow_Encoder.byte -> {
                tmpInfo.motor_id = CareRobotMC.Left_Shoulder.byte
                tmpInfo.max_Range = 190f
                tmpInfo.min_Range = 15f
                if (exPositonhmap[id]!! < position) {
                    tmpInfo.currnet_Direction = Direction.CCW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else if (exPositonhmap[id]!! > position) {
                    tmpInfo.currnet_Direction = Direction.CW
                    exDirection[id] = tmpInfo.currnet_Direction!!
                } else {
                    tmpInfo.currnet_Direction = exDirection[id]
                }
                // 에러 범위 안인가?
                if (tmpInfo.position!! < tmpInfo.min_Range!!) {
                    if (viewModel.controlDirection != Direction.CCW) {
                        tmpInfo.min_Alert = true
                    }
                } else if (tmpInfo.position!! > tmpInfo.max_Range!!) {
                    if (viewModel.controlDirection != Direction.CW) {
                        tmpInfo.max_Alert = true
                    }
                }
            }
        }

        viewModel.motorInfo[id] = tmpInfo
        exPositonhmap[id] = position
    }
}