package com.samin.carerobot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.samin.carerobot.Logics.SharedPreference
import com.samin.carerobot.Logics.SharedViewModel
import com.samin.carerobot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var mBinding: ActivityMainBinding
    lateinit var loginFragment: LoginFragment
    lateinit var newAccountFragment: NewAccountFragment
    lateinit var mainFragment: MainFragment
    lateinit var sharedPreference: SharedPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setFragment()
        sharedPreference = SharedPreference(this)
    }

    private fun setFragment() {
        loginFragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.HostFragment_container, loginFragment).commit()
        newAccountFragment = NewAccountFragment()
        mainFragment = MainFragment()
    }

    fun onFragmentChange(index:Int){
        when(index){
            SharedViewModel.LOGINFRAGMENT -> supportFragmentManager.beginTransaction().replace(R.id.HostFragment_container, loginFragment).commit()
            SharedViewModel.NEWACCOUNTFRAGMENT ->supportFragmentManager.beginTransaction().replace(R.id.HostFragment_container, newAccountFragment).commit()
            SharedViewModel.MAINFRAGMENT ->supportFragmentManager.beginTransaction().replace(R.id.HostFragment_container, mainFragment).commit()

        }

    }
}