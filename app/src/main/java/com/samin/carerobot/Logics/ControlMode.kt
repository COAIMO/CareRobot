package com.samin.carerobot.Logics

import com.jeongmin.nurimotortester.Nuri.EnumCodesMap
import com.jeongmin.nurimotortester.Nuri.ProtocolMode

enum class ControlMode(val byte: Byte) {
    Left(1),
    Right(2),
    Both(3);
    companion object : EnumCodesMap<ProtocolMode, Byte> by EnumCodesMap({ it.byte })
}