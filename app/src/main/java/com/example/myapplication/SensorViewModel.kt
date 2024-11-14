import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SensorViewModel(
    private val sensorManager: SensorManager,
    private val wifiManager: WifiManager,
    private val context: Context
) : ViewModel(), SensorEventListener {

    private val _accelerometerData = MutableStateFlow("Waiting for accelerometer data...")
    private val _gyroscopeData = MutableStateFlow("Waiting for gyroscope data...")
    private val _magnetometerData = MutableStateFlow("Waiting for magnetometer data...")
    private val _wifiData = MutableStateFlow("Waiting for Wi-Fi data...")

    val accelerometerData: StateFlow<String> = _accelerometerData
    val gyroscopeData: StateFlow<String> = _gyroscopeData
    val magnetometerData: StateFlow<String> = _magnetometerData
    val wifiData: StateFlow<String> = _wifiData

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        startWifiScanning()  // No context needed as it’s now a class property
    }

    fun startSensorUpdates() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val sensorType = it.sensor.type
            val data = when (sensorType) {
                Sensor.TYPE_ACCELEROMETER -> "X=%.3f, Y=%.3f, Z=%.3f".format(it.values[0], it.values[1], it.values[2])
                Sensor.TYPE_GYROSCOPE -> "X=%.3f, Y=%.3f, Z=%.3f".format(it.values[0], it.values[1], it.values[2])
                Sensor.TYPE_MAGNETIC_FIELD -> "X=%.3f, Y=%.3f, Z=%.3f".format(it.values[0], it.values[1], it.values[2])
                else -> "Unknown Sensor"
            }

            viewModelScope.launch(Dispatchers.Default) {
                when (sensorType) {
                    Sensor.TYPE_ACCELEROMETER -> _accelerometerData.value = data
                    Sensor.TYPE_GYROSCOPE -> _gyroscopeData.value = data
                    Sensor.TYPE_MAGNETIC_FIELD -> _magnetometerData.value = data
                }
                Log.d("Sensor Data:", data)
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

    // Function to start periodic Wi-Fi scanning
    private fun startWifiScanning() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                // Check if location permission is granted
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val success = wifiManager.startScan()
                    if (success) {
                        val scanResults: List<ScanResult> = wifiManager.scanResults

                        val wifiInfo = scanResults.joinToString(separator = "\n") { result ->
                            "${result.SSID}: ${result.level} dBm"
                        }
                        _wifiData.value = wifiInfo
                        Log.d("WiFi Data", wifiInfo)
                    } else {
                        Log.d("WiFi Data", "Scan failed")
                    }
                } else {
                    Log.d("WiFi Data", "Location permission not granted")
                }
                delay(10000)  /* 10 sec */
            }
        }
    }
}

class SensorViewModelFactory(
    private val sensorManager: SensorManager,
    private val wifiManager: WifiManager,
    private val context: Context  // Add context here for ViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorViewModel(sensorManager, wifiManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
