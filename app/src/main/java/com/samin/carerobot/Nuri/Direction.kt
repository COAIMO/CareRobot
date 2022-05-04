package com.jeongmin.nurimotortester.Nuri

enum class Direction(val direction: Byte) {
    CCW(0),
    CW(1);
    companion object : EnumCodesMap<Direction, Byte> by EnumCodesMap({ it.direction })
}