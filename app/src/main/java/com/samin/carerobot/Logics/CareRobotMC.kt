package com.samin.carerobot.Logics

import com.jeongmin.nurimotortester.Nuri.EnumCodesMap
import com.jeongmin.nurimotortester.Nuri.ProtocolMode

enum class CareRobotMC(val byte: Byte) {
    Left_Wheel(0),
    Right_Wheel(1),
    Leg_Angle(2),
    Waist(3),
    Left_Shoulder(4),
    Right_Shoulder(5),
    Left_Elbow(6),
    Right_Elbow(7),
    Eyes_Display(8);
    companion object : EnumCodesMap<ProtocolMode, Byte> by EnumCodesMap({ it.byte })
}