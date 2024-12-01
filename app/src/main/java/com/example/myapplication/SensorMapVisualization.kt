package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.translate


@Composable
fun SensorMapVisualization(
    corners: List<Offset>,
    currentPosition: Offset,
    heading: Float,
    modifier: Modifier = Modifier,
    zoomLevel: Float = 1.25f
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenCenter = Offset(size.width / 2, size.height / 2)

            // Fix path to arrow's base at screen center, then rotate
            translate(screenCenter.x - currentPosition.x * zoomLevel, screenCenter.y - currentPosition.y * zoomLevel) {
                if (corners.size > 1) {
                    corners.zipWithNext { a, b ->
                        drawLine(
                            color = Color.Red,
                            start = a,
                            end = b,
                            strokeWidth = 5f
                        )
                    }
                }
            }
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .rotate(heading),
            tint = Color.Blue
        )
    }
}




private fun parseWifiData(wifiLine: String): Pair<String, Int> {
    val parts = wifiLine.split(":")
    return if (parts.size >= 2) {
        val ssid = parts[0].trim()
        val strength = parts[1].trim().replace(" dBm", "").toIntOrNull() ?: -100
        Pair(ssid, strength)
    } else {
        Pair("Unknown", -100)
    }
}

private fun calculateWifiPosition(strength: Int): androidx.compose.ui.geometry.Offset {
    // Convert dBm to rough distance estimate
    val distance = ((-60 - strength) * 2f).coerceIn(20f, 200f)
    val angle = strength.hashCode() % 360 * (Math.PI / 180)

    return androidx.compose.ui.geometry.Offset(
        x = distance * cos(angle).toFloat(),
        y = distance * sin(angle).toFloat()
    )
}
