package com.jeongmin.nurimotortester.Nuri


enum class Baudrate(val buad_rate : Int) {
    BR_100(110),
    BR_300(300),
    BR_600(600),
    BR_1200(1200),
    BR_2400(2400),
    BR_4800(4800),
    BR_9600(9600),
    BR_14400(14400),
    BR_19200(19200),
    BR_28800(28800),
    BR_38400(38400),
    BR_57600(57600),
    BR_76800(76800),
    BR_115200(115200),
    BR_230400(230400),
    BR_250000(250000),
    BR_500000(500000),
    BR_1000000(1000000);
    companion object : EnumCodesMap<Baudrate, Int> by EnumCodesMap({ it.buad_rate })

}

