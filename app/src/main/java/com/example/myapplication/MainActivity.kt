package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon


data class RoomLayout(
    val corners: List<androidx.compose.ui.geometry.Offset>,
    val timestamp: Long,
    val wifiReferences: List<WifiReference> = emptyList()
)

data class WifiReference(
    val ssid: String,
    val strength: Int,
    val estimatedPosition: androidx.compose.ui.geometry.Offset
)

class MainActivity : ComponentActivity() {
    private val roomLayoutViewModel: RoomLayoutViewModel by viewModels()
    private lateinit var sensorViewModel: SensorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        val sensorViewModelFactory = SensorViewModelFactory(
            sensorManager = sensorManager,
            wifiManager = wifiManager,
            context = this,
            roomLayoutViewModel = roomLayoutViewModel
        )

        sensorViewModel = ViewModelProvider(this, sensorViewModelFactory)[SensorViewModel::class.java]

        setContent {
            val corners by roomLayoutViewModel.corners.collectAsState()
            val currentPosition by roomLayoutViewModel.currentPosition.collectAsState()
            val heading by roomLayoutViewModel.heading.collectAsState()

            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FloatingActionButton(
                                onClick = { sensorViewModel.resetTracking() }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Reset Tracking"
                                )
                            }
                        }
                    }
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    ) {
                        SensorMapVisualization(
                            corners = corners,
                            currentPosition = currentPosition,
                            heading = heading,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}