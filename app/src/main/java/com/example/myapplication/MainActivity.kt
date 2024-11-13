package com.example.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var sensorViewModel: SensorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager != null) {
            val factory = SensorViewModelFactory(sensorManager)
            sensorViewModel = ViewModelProvider(this, factory).get(SensorViewModel::class.java)
        } else {
            Log.e("MainActivity", "SensorManager is not available.")
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        CameraPreview(modifier = Modifier.weight(1f))

                        val accelerometerData by sensorViewModel.accelerometerData.collectAsState()
                        val gyroscopeData by sensorViewModel.gyroscopeData.collectAsState()
                        val magnetometerData by sensorViewModel.magnetometerData.collectAsState()

                        SensorDisplay("Accelerometer: $accelerometerData")
                        SensorDisplay("Gyroscope: $gyroscopeData")
                        SensorDisplay("Magnetometer: $magnetometerData")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorViewModel.stopSensorUpdates()
    }

    override fun onResume() {
        super.onResume()
        sensorViewModel.startSensorUpdates()
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

        AndroidView(modifier = modifier.fillMaxSize(), factory = { ctx ->
            val previewView = androidx.camera.view.PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context as ComponentActivity, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            previewView
        })
    } else {
        Text("Camera permission required", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun SensorDisplay(sensorData: String, modifier: Modifier = Modifier) {
    Text(text = sensorData, modifier = modifier.padding(16.dp))
}
