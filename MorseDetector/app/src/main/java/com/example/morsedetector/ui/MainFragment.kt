package com.example.morsedetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.morsedetector.R
import com.example.morsedetector.util.Constants
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment: BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnDecode?.setOnClickListener {
            navController?.navigate(R.id.action_fragmentMain_to_fragmentDecoder)
        }
        btnGenerate?.setOnClickListener {
            navController?.navigate(R.id.action_fragmentMain_to_fragmentSoundGenerator)
        }
        requestNeededPermissions()
    }

    private fun requestNeededPermissions() {
        val permissions = RxPermissions(this)
        val nonGrantedPermissions = getNeededPermissions()

        if (nonGrantedPermissions.isNotEmpty()) {
            permissions.requestEachCombined(*(nonGrantedPermissions.toTypedArray()))
                .subscribe { permission ->

                }
        }
    }
    private fun getNeededPermissions(): List<String> {
        val permissions = RxPermissions(this)
        val nonGrantedPermissions = Constants.neededPermissions.filter {
            !permissions.isGranted(it)
        }
        return nonGrantedPermissions
    }
}