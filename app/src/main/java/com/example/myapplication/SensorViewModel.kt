package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorViewModel(
    private val sensorManager: SensorManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), SensorEventListener {
    private var isCollecting = false
    private val sensorFusion = SensorFusion()
    private val _data = MutableStateFlow(
        savedStateHandle.get<SensingData>("sensing_data") ?: SensingData()
    )
    private val _error = MutableStateFlow<String?>(null)
    private var lastAcceleration = FloatArray(3)
    private var lastGyroscope = FloatArray(3)
    private var hasAccelData = false
    private var hasGyroData = false

    val data: StateFlow<SensingData> = _data.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        initializeSensors()
    }

    fun startCollecting() {
        isCollecting = true
    }

    fun stopCollecting() {
        isCollecting = false
    }

    private fun initializeSensors() {
        try {
            val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

            requireNotNull(accelSensor) { "Linear acceleration sensor not available" }
            requireNotNull(gyroSensor) { "Gyroscope sensor not available" }

            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)

        } catch (e: Exception) {
            _error.value = "Sensor initialization failed: ${e.message}"
            Log.e("SensorViewModel", "Sensor initialization failed", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isCollecting) return
        try {
            when (event?.sensor?.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    lastAcceleration = event.values.clone()
                    hasAccelData = true
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyroscope = event.values.clone()
                    hasGyroData = true
                }
            }

            if (hasAccelData && hasGyroData) {
                val newData = sensorFusion.process(lastAcceleration, lastGyroscope)
                _data.value = newData
                savedStateHandle["sensing_data"] = newData
                hasAccelData = false
                hasGyroData = false
            }
        } catch (e: Exception) {
            _error.value = "Processing error: ${e.message}"
            Log.e("SensorViewModel", "Processing error", e)
        }
    }

    fun reset() {
        sensorFusion.reset()
        _data.value = SensingData()
        _error.value = null
        hasAccelData = false
        hasGyroData = false
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}