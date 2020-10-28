package com.example.morsedetector.ui

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController

abstract class BaseFragment : Fragment() {

    val navController: NavController by lazy {
        findNavController()
    }

}