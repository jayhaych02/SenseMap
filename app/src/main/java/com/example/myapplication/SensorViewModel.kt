package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SensorViewModel(
    private val sensorManager: SensorManager  // SensorManager is passed to the ViewModel
) : ViewModel(), SensorEventListener {

    private val _sensorData = MutableStateFlow("Waiting for sensor data...")
    val sensorData: StateFlow<String> = _sensorData



    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var proximity: Sensor? = null

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    fun startSensorUpdates() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        proximity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val sensorType = it.sensor.type
            val data = when (sensorType) {
                Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD -> {
                    if (it.values.size >= 3) "X=${it.values[0]}, Y=${it.values[1]}, Z=${it.values[2]}"
                    else "Insufficient data"
                }
                Sensor.TYPE_PROXIMITY -> "Proximity: ${it.values[0]}"
                else -> "Unknown Sensor"
            }

            viewModelScope.launch(Dispatchers.Default) {
                _sensorData.value = data
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val accuracyMessage = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Sensor accuracy is high"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Sensor accuracy is medium"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Sensor accuracy is low"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "Sensor accuracy is unreliable"
            else -> "Sensor accuracy is unknown"
        }
        Log.d("SensorAccuracy", accuracyMessage)
    }
}

class SensorViewModelFactory(
    private val sensorManager: SensorManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorViewModel(sensorManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
