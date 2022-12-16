package com.samin.carerobot.Logics

import com.jeongmin.nurimotortester.Nuri.EnumCodesMap
import com.jeongmin.nurimotortester.Nuri.ProtocolMode

enum class CareRobotMC(val byte: Byte) {
    Left_Wheel(6),
    Right_Wheel(7),
//    Leg_Angle(8),
    Waist(5),
    Left_Shoulder(1),
    Right_Shoulder(3),
//    Left_Elbow(2),
//    Right_Elbow(4),
//    Leg_Sensor(10),
    Left_Shoulder_Encoder(11),
    Right_Shoulder_Encoder(13),
//    Left_Elbow_Encoder(12),
//    Right_Elbow_Encoder(14),
    Waist_Sensor(15),
//    Elbow(16),
    Wheel(17),
    Eyes_Display(18);
    companion object : EnumCodesMap<ProtocolMode, Byte> by EnumCodesMap({ it.byte })
}
