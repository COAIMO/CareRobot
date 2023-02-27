package com.samin.carerobot.Logics

import com.jeongmin.nurimotortester.Nuri.EnumCodesMap

enum class WheelSpeed(val speed: Float) {
    //원래 First 29f ->28f변경(속도 시험)
    First(28f),
    Second(15f),
    Third(7.5f);
    companion object : EnumCodesMap<WheelSpeed, Float> by EnumCodesMap({ it.speed })
}