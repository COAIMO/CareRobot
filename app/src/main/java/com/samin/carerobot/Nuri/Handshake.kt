package com.jeongmin.nurimotortester.Nuri


enum class Handshake(handshake: Int) {
    /// <summary>
    /// 핸드셰이크에 제어 없음
    /// </summary>
    None(0),
    /// <summary>
    /// XON/XOFF 소프트웨어 제어 프로토콜을 사용
    /// </summary>
    XOnXoff(1),
    /// <summary>
    /// RTS(Request to Send) 하드웨어 흐름 제어를 사용
    /// </summary>
    RTS(2),
    /// <summary>
    /// RTS(Request to Send) 하드웨어 제어와 XON/XOFF 소프트웨어 제어를 모두 사용
    /// </summary>
    RTSXOnXoff(3)
}