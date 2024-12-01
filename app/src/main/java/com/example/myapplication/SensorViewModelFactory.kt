package com.example.myapplication

import android.content.Context
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SensorViewModelFactory(
    private val sensorManager: SensorManager,
    private val wifiManager: WifiManager,
    private val context: Context,
    private val roomLayoutViewModel: RoomLayoutViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            return SensorViewModel(
                sensorManager = sensorManager,
                wifiManager = wifiManager,
                context = context,
                roomLayoutViewModel = roomLayoutViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
