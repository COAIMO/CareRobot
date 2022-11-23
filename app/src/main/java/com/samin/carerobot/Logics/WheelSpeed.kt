package com.samin.carerobot.Logics

import com.jeongmin.nurimotortester.Nuri.EnumCodesMap

enum class WheelSpeed(val speed: Float) {
    First(29f),
    Second(15f),
    Third(7.5f);
    companion object : EnumCodesMap<WheelSpeed, Float> by EnumCodesMap({ it.speed })
}