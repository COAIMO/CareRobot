package com.samin.carerobot.Logics

import com.jeongmin.nurimotortester.Nuri.Direction

data class MotorInfo(
    var motor_id : Byte? = null,
    var encoder_id : Byte? = null,
    var currnet_Direction:Direction? = null,
    var position:Float? = null,
    var isStop:Boolean? = true,
    var min_Range:Float? = null,
    var max_Range:Float? = null,
    var min_Alert:Boolean? = false,
    var max_Alert:Boolean? = false,
    var proximity_Sensor:Boolean? = null,
    var sensorData:Byte? = null
)
