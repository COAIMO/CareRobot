package com.samin.carerobot.Logics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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
}