package com.samin.carerobot.Nuri

interface ICommand {
    var PacketName: String?
    var Data : ByteArray?
    var ID : Byte?
    fun Parse(data: ByteArray): Boolean
    fun GetCheckSum():Byte
    fun GetDataStruct(): Any
}