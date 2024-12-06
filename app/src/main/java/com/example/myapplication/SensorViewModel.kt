package com.example.myapplication

import android.hardware.SensorManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorViewModel(
    sensorManager: SensorManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val sensorFusion = SensorFusion(sensorManager)
    private val _data = MutableStateFlow(
        savedStateHandle.get<SensingData>("sensing_data") ?: SensingData()
    )
    private val _error = MutableStateFlow<String?>(null)

    val data: StateFlow<SensingData> = _data.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        sensorFusion.setOnDataUpdatedListener { newData ->
            _data.value = newData
            savedStateHandle["sensing_data"] = newData
        }
    }

    fun startCollecting() {
        sensorFusion.start()
    }

    fun stopCollecting() {
        sensorFusion.stop()
    }

    fun reset() {
        sensorFusion.reset()
        _data.value = SensingData()
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        sensorFusion.cleanup()
    }
}