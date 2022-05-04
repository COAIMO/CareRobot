package com.jeongmin.nurimotortester.Nuri


class AppState {
    /// <summary>
    /// 시작 팝업 사용 여부
    /// </summary>
    var IsUseStartPopup : Boolean = true

    /// <summary>
    /// 데이터 비트
    /// </summary>
    var Databits : Int = 8

    /// <summary>
    /// 제어 프로토콜
    /// </summary>
    var Handshake : Int = 0

    /// <summary>
    /// 패리티
    /// </summary>
    var Parity :Int = 0

    /// <summary>
    /// 정지 비트
    /// </summary>
    var StopBits: Int =1

    /// <summary>
    /// 읽기 타임아웃
    /// </summary>
    var ReadTimeout:Int = 10

    /// <summary>
    /// 쓰기 타임아웃
    /// </summary>
    var WriteTimeout:Int = 10

    /// <summary>
    /// 선택된 시리얼 포트
    /// </summary>
    var Comport:String? =null

    /// <summary>
    /// 선택된 연결 속도
    /// </summary>
    var Baudrate:String? =null

    /// <summary>
    /// 시리얼 연결 상태
    /// </summary>
    var IsConnect:Boolean? = null

    /// <summary>
    /// 프로그램 테마
    /// </summary>
    var ColorTheme:String ="Dark.Blue"

    /// <summary>
    /// 언어설정
    /// </summary>
    var Language:String = "ko"

    /// <summary>
    /// 검색된 연결 장비
    /// </summary>
    var SearchDevice:List<Byte>? = null

}