package com.jeongmin.nurimotortester.Nuri

enum class ProtocolModeRSA(val byte: Byte) {
    /// <summary>
    /// 없음
    /// </summary>
    CTRLNone(0x00),

    /// <summary>
    /// 위치, 속도제어(송신)
    /// </summary>
    CTRLPosSpeed(0x01),

    /// <summary>
    /// 가감속 위치제어(송신)
    /// </summary>
    CTRLAccPos(0x02),

    /// <summary>
    /// 가감속 속도제어(송신)
    /// </summary>
    CTRLAccSpeed(0x03),
    /// <summary>
    /// 위치제어기 설정(송신)
    /// </summary>
    SETPosCtrl(0x04),

    /// <summary>
    /// 속도제어기 설정(송신)
    /// </summary>
    SETSpeedCtrl(0x05),

    /// <summary>
    /// ID설정(송신)
    /// </summary>
    SETID(0x06),

    /// <summary>
    /// 통신속도 설정(송신)
    /// </summary>
    SETBaudrate(0x07),

    /// <summary>
    /// 통신 응답시간 설정(송신)
    /// </summary>
    SETResptime(0x08),

    /// <summary>
    /// 감속비 설정(송신)
    /// </summary>
    SETRatio(0x09),

    /// <summary>
    /// 제어 On/Off 설정(송신)
    /// </summary>
    SETCtrlOnOff(0x0A),

    /// <summary>
    /// 위치제어모드 설정(송신)
    /// </summary>
    SETPosCtrlMode(0x0B),

    /// <summary>
    /// 위치초기화(송신)
    /// </summary>
    RESETPos(0x0C),

    /// <summary>
    /// 공장 초기화(송신)
    /// </summary>
    RESETFactory(0x0D),

    /// <summary>
    /// 핑
    /// </summary>
    REQPing(0xA0.toByte()),

    /// <summary>
    /// 위치 피드백
    /// </summary>
    REQPos(0xA1.toByte()),

    /// <summary>
    /// 속도 피드백
    /// </summary>
    REQSpeed(0xA2.toByte()),

    /// <summary>
    /// 위치제어기 피드백
    /// </summary>
    REQPosCtrl(0xA3.toByte()),

    /// <summary>
    /// 속도제어기 피드백
    /// </summary>
    REQSpdCtrl(0xA4.toByte()),

    /// <summary>
    /// 통신 응답시간 피드백
    /// </summary>
    REQResptime(0xA5.toByte()),

    /// <summary>
    /// 감속비 피드백
    /// </summary>
    REQRatio(0xA6.toByte()),

    /// <summary>
    /// 제어 On/Off 피드백
    /// </summary>
    REQCtrlOnOff(0xA7.toByte()),

    /// <summary>
    /// 위치제어모드 피드백
    /// </summary>
    REQPosCtrlMode(0xA8.toByte()),

    /// <summary>
    /// 펌웨어 버전 피드백
    /// </summary>
    REQFirmware(0xCD.toByte()),

    /// <summary>
    /// 핑(수신)
    /// </summary>
    FEEDPing(0xD0.toByte()),

    /// <summary>
    /// 위치 피드백 (수신)
    /// </summary>
    FEEDPos(0xD1.toByte()),

    /// <summary>
    /// 속도 피드백(수신)
    /// </summary>
    FEEDSpeed(0xD2.toByte()),

    /// <summary>
    /// 위치제어기 피드백(수신)
    /// </summary>
    FEEDPosCtrl(0xD3.toByte()),

    /// <summary>
    /// 속도제어기 피드백(수신)
    /// </summary>
    FEEDSpdCtrl(0xD4.toByte()),

    /// <summary>
    /// 통신 응답시간 피드백(수신)
    /// </summary>
    FEEDResptime(0xD5.toByte()),

    /// <summary>
    /// 감속비 피드백(수신)
    /// </summary>
    FEEDRatio(0xD6.toByte()),

    /// <summary>
    /// 제어On/Off 피드백(수신)
    /// </summary>
    FEEDCtrlOnOff(0xD7.toByte()),

    /// <summary>
    /// 위치제어 모드 피드백(수신)
    /// </summary>
    FEEDPosCtrlMode(0xD8.toByte()),

    /// <summary>
    /// 펌웨어 버전 피드백(수신)
    /// </summary>
    FEEDFirmware(0xFD.toByte());

    companion object : EnumCodesMap<ProtocolModeRSA, Byte> by EnumCodesMap({ it.byte })

}