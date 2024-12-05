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

    val data: StateFlow<SensingData> = _data.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        initializeSensor()
    }

    fun startCollecting() {
        isCollecting = true
    }

    fun stopCollecting() {
        isCollecting = false
    }

    private fun initializeSensor() {
        try {
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            requireNotNull(sensor) { "Linear acceleration sensor not available" }

            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            require(registered) { "Failed to register sensor listener" }
        } catch (e: Exception) {
            _error.value = "Sensor initialization failed: ${e.message}"
            Log.e("SensorViewModel", "Sensor initialization failed", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isCollecting) return
        try {
            event?.takeIf { it.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION }?.let {
                val newData = sensorFusion.process(it.values)
                _data.value = newData
                savedStateHandle["sensing_data"] = newData
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
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}