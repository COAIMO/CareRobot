package com.jeongmin.nurimotortester.Nuri

enum class BaudrateByte(val byte: Byte) {
    BR_110(0x00),
    BR_300(0x01),
    BR_600(0x02),
    BR_1200(0x03),
    BR_2400(0x04),
    BR_4800(0x05),
    BR_9600 (0x06),
    BR_14400(0x07),
    BR_19200(0x08),
    BR_28800(0x09),
    BR_38400(0x0A),
    BR_57600(0x0B),
    BR_76800(0x0C),
    BR_115200(0x0D),
    BR_230400(0x0E),
    BR_250000(0x0F),
    BR_500000(0x10),
    BR_1000000(0x11);
    companion object : EnumCodesMap<BaudrateByte, Byte> by EnumCodesMap({ it.byte })
}