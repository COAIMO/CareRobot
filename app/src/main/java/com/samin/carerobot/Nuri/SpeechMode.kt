package com.samin.carerobot.Nuri

import com.jeongmin.nurimotortester.Nuri.EnumCodesMap

enum class SpeechMode(val byte: Byte) {
    TOUCH_WAITTING_SCREEN(0x01),
    TOUCH_MIC_ON(0x02),
    TOUCH_MIC_OFF(0X03),
    TOUCH_CARRYMODE(0x04),
    TOUCH_BEHAVIORMODE(0x05),
    TOUCH_CHANGEMODE(0x06),
    TOUCH_ALLMODE(0x07),
    TOUCH_BUTTON_1(0x08),
    TOUCH_BUTTON_2(0x09);
    companion object : EnumCodesMap<SpeechMode, Byte> by EnumCodesMap({ it.byte })
}