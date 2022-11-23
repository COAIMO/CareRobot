package com.samin.carerobot.Logics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jeongmin.nurimotortester.Nuri.Direction
import com.jeongmin.nurimotortester.Nuri.NuriPosSpeedAclCtrl
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SharedViewModel:ViewModel() {
    companion object{
        const val LOGINFRAGMENT = 0
        const val NEWACCOUNTFRAGMENT = 1
        const val MAINFRAGMENT = 2
        const val SELECTMODEFRAGMENT =3
        const val SELECTEDMODEFRAGMENT =4
        const val MODE_CARRY = 5
        const val MODE_CARRY_HEAVY = 6
        const val MODE_CARRY_HEIGHT = 7

        const val MODE_BEHAVIOR = 8
        const val MODE_BEHAVIOR_STAND = 9
        const val MODE_BEHAVIOR_WALKHAND = 10
        const val MODE_BEHAVIOR_WALKHUG = 11

        const val MODE_CHANGE = 12
        const val MODE_CHANGE_CHANGEHUG = 13
        const val MODE_CHANGE_TRANSFERSTAND = 14
        const val MODE_CHANGE_TRANSFERHARNESS = 15

        const val MODE_ALL = 16
        const val MODE_ALL_POSITION = 17
        const val MODE_ALL_CHANGESLING = 18
        const val MODE_ALL_TRANSFERSLING = 19
        const val MODE_ALL_TRANSFERBEDRIDDENSLING = 20
        const val MODE_ALL_TRANSFERBEDRIDDENBOARD = 21
        const val MODE_ALL_TRANSFERCHAIR = 22

    }

    var viewState = MutableLiveData<Int>()
    var left_Joystick = MutableLiveData<JoystickCoordinate>()
    var right_Joystick = MutableLiveData<JoystickCoordinate>()
    var controlPart = MutableLiveData<Byte?>()
    val motorInfo = ConcurrentHashMap<Byte,MotorInfo>()
    val posInfos = ConcurrentHashMap<Byte, Float>()
    var controlDirection = Direction.CW
//    val waistInfo = MutableLiveData<NuriPosSpeedAclCtrl>()
    val waistInfo = MutableLiveData<MotorInfo>()
    val lockobj = Object()
    val isControlMap = ConcurrentHashMap<Byte,Boolean>()
    val isControl = AtomicInteger(0)

    val waistStateMap = ConcurrentHashMap<Byte, Int>()
    val sendProtocolMap = ConcurrentHashMap<Byte, ByteArray>()
    val exProtocolMap = ConcurrentHashMap<Byte, ByteArray>()
    val usingMic = MutableLiveData<Boolean>()
    var wheelMaxSpeed :Float = WheelSpeed.First.speed

}