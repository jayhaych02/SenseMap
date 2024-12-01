package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel

class SensorViewModel(
    private val sensorManager: SensorManager,
    private val wifiManager: WifiManager,
    private val context: Context,
    private val roomLayoutViewModel: RoomLayoutViewModel
) : ViewModel(), SensorEventListener {

    private val sensorFusion = SensorFusion()

    init {
        startSensorUpdates()
    }

    private fun startSensorUpdates() {
        // Register sensors
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    // Process rotation vector for heading updates
                    val rotationMatrix = FloatArray(9)
                    val orientation = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    sensorFusion.updateHeading(azimuth)
                    roomLayoutViewModel.updateHeading(azimuth)
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    // Process steps to update position
                    sensorFusion.processStep()
                    val position = sensorFusion.getCurrentPosition()
                    roomLayoutViewModel.updateCurrentPosition(position)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    // Process accelerometer for finer movement tracking
                    val offset = sensorFusion.processAccelerometer(
                        it.values[0], it.values[1], it.values[2]
                    )
                    roomLayoutViewModel.updateCurrentPosition(offset)
                }
            }
        }
    }
    fun resetTracking() {
        sensorFusion.reset()
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this) // Unregister sensors when ViewModel is cleared
    }
}


