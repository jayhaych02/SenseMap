package com.example.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.hardware.SensorManager
import android.net.wifi.WifiManager
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

class SensorMapVisualization {

    data class DevicePosition(
        val x: Float,
        val y: Float,
        val rotation: Float,
        val wifiPoints: List<WifiPoint> = emptyList()
    )

    data class WifiPoint(
        val ssid: String,
        val strength: Float, // Signal strength in dBm
        val distance: Float  // Estimated distance based on signal strength
    )

    @Composable
    fun SensorMapVisualizationScreen(
        accelerometerData: String,
        gyroscopeData: String,
        wifiData: String,
        modifier: Modifier = Modifier
    ) {
        var devicePosition by remember { mutableStateOf(DevicePosition(250f, 250f, 0f)) }
        val pathPoints = remember { mutableStateListOf<DevicePosition>() }

        // Parse sensor data
        LaunchedEffect(accelerometerData, gyroscopeData, wifiData) {
            // Extract accelerometer values
            val accValues = accelerometerData.substringAfter(":").trim()
                .split(",").map { it.substringAfter("=").trim().toFloatOrNull() ?: 0f }

            // Simple integration of acceleration to position
            if (accValues.size >= 2) {
                val scaleFactor = 0.1f // Adjust this to control sensitivity
                val newX = devicePosition.x + accValues[0] * scaleFactor
                val newY = devicePosition.y + accValues[1] * scaleFactor

                // Extract rotation from gyroscope
                val gyroValues = gyroscopeData.substringAfter(":").trim()
                    .split(",").map { it.substringAfter("=").trim().toFloatOrNull() ?: 0f }
                val rotation = if (gyroValues.isNotEmpty()) {
                    devicePosition.rotation + gyroValues[0] * scaleFactor
                } else devicePosition.rotation

                // Parse WiFi data
                val wifiPoints = wifiData.split("\n").map { wifiLine ->
                    val parts = wifiLine.split(":")
                    if (parts.size >= 2) {
                        val ssid = parts[0].trim()
                        val strength = parts[1].trim().replace(" dBm", "").toFloatOrNull() ?: -100f
                        // Convert dBm to estimated distance (very rough approximation)
                        val distance = calculateDistance(strength)
                        WifiPoint(ssid, strength, distance)
                    } else null
                }.filterNotNull()

                val newPosition = DevicePosition(newX, newY, rotation, wifiPoints)
                devicePosition = newPosition
                pathPoints.add(newPosition)

                // Keep only recent history
                if (pathPoints.size > 100) {
                    pathPoints.removeAt(0)
                }
            }
        }

        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            // Draw movement trail
            pathPoints.zipWithNext { a, b ->
                drawLine(
                    color = Color.Blue.copy(alpha = 0.3f),
                    start = Offset(a.x, a.y),
                    end = Offset(b.x, b.y),
                    strokeWidth = 2f
                )
            }

            // Draw current device position
            drawDevice(devicePosition)

            // Draw WiFi points
            devicePosition.wifiPoints.forEach { wifiPoint ->
                drawWifiPoint(devicePosition, wifiPoint)
            }
        }
    }


    private fun DrawScope.drawDevice(position: DevicePosition) {
        // Draw device as a triangle pointing in movement direction
        val size = 20f
        val points = listOf(
            Offset(
                position.x + size * cos(position.rotation),
                position.y + size * sin(position.rotation)
            ),
            Offset(
                position.x + size * cos(position.rotation + 2.3f),
                position.y + size * sin(position.rotation + 2.3f)
            ),
            Offset(
                position.x + size * cos(position.rotation - 2.3f),
                position.y + size * sin(position.rotation - 2.3f)
            )
        )

        drawCircle(
            color = Color.Red,
            radius = 5f,
            center = Offset(position.x, position.y)
        )

        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points[0].x, points[0].y)
                lineTo(points[1].x, points[1].y)
                lineTo(points[2].x, points[2].y)
                close()
            },
            color = Color.Red.copy(alpha = 0.7f)
        )
    }

    private fun DrawScope.drawWifiPoint(devicePosition: DevicePosition, wifiPoint: WifiPoint) {
        // Position WiFi points in a circle around the device based on signal strength
        val angle = wifiPoint.ssid.hashCode() % 360 * (Math.PI / 180)
        val x = devicePosition.x + wifiPoint.distance * cos(angle).toFloat()
        val y = devicePosition.y + wifiPoint.distance * sin(angle).toFloat()

        drawCircle(
            color = Color.Green.copy(alpha = 0.5f),
            radius = 10f,
            center = Offset(x, y)
        )

        // Draw connection line
        drawLine(
            color = Color.Green.copy(alpha = 0.2f),
            start = Offset(devicePosition.x, devicePosition.y),
            end = Offset(x, y),
            strokeWidth = 1f
        )
    }

    private fun calculateDistance(signalStrength: Float): Float {
        // Very rough approximation of distance based on signal strength
        // You might want to use a more sophisticated model
        val minDistance = 20f
        val maxDistance = 200f
        val signalRange = 40f // Typical range from -100 to -60 dBm

        return (((-60f - signalStrength) / signalRange) * (maxDistance - minDistance) + minDistance)
            .coerceIn(minDistance, maxDistance)
    }
}
