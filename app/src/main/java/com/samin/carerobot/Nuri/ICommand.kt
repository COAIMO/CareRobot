package com.samin.carerobot.Nuri

interface ICommand {
    var packet: Byte?
    var Data : ByteArray?
    var ID : Byte?
    fun Parse(data: ByteArray): Boolean
    fun GetCheckSum():Byte
    fun GetDataStruct(): Any
}