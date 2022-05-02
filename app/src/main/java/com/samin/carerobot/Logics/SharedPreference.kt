package com.samin.carerobot.Logics

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreference(context:Context) {

    companion object{
        const val USER_NAME ="NAME"
    }

    private val  userInfoSharedPreference =
        context.getSharedPreferences("user", Context.MODE_PRIVATE)

    fun saveUserInfo(key:String, data:String){
        val userInfo = Gson().toJson(data)
        userInfoSharedPreference.edit().apply{
            putString(key, userInfo)
            apply()
        }
    }

    fun checkUserID(input_id: String):Boolean{
        val tmp = userInfoSharedPreference.contains(input_id)
        return tmp
    }
    fun checkUserPassword(input_id: String, input_password: String):Boolean{
        var tmp = false
        val data = userInfoSharedPreference.getString(input_id, "")!!
        val token = object : TypeToken<String>() {}.type
        if (data.isNotEmpty()){
            val password = Gson().fromJson<String>(data, token)
            tmp = password == input_password
        }
        return tmp
    }

    fun loadUserID():String{
        var tmp = ""
        val data = userInfoSharedPreference.getString(USER_NAME, "")!!
        val token = object : TypeToken<String>() {}.type
        if (data.isNotEmpty()){
            tmp = Gson().fromJson<String>(data, token)
        }
        return tmp
    }

}