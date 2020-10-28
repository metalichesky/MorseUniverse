package com.example.morsedetector

import android.app.Application
import androidx.fragment.app.Fragment
import com.tbruyelle.rxpermissions3.RxPermissions

class App: Application() {
    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun requestPermissions(fragment: Fragment, vararg permissions: String) {
        val rxPermissions = RxPermissions(fragment)
        val nonGrantedPermissions = mutableListOf<String>()
        permissions.forEach {
            if (!rxPermissions.isGranted(it)) {
                nonGrantedPermissions.add(it)
            }
        }
        rxPermissions.requestEach(*nonGrantedPermissions.toTypedArray())
    }
}