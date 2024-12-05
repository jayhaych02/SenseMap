package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: SensorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        viewModel = ViewModelProvider(this@MainActivity, object : AbstractSavedStateViewModelFactory(
            this@MainActivity, savedInstanceState
        ) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T = SensorViewModel(sensorManager, handle) as T
        })[SensorViewModel::class.java]

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var timeRemaining by remember { mutableStateOf(10) }
                    var showResults by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        viewModel.startCollecting()
                        viewModel.reset()

                        repeat(10) {
                            delay(1000)
                            timeRemaining = 9 - it
                        }

                        viewModel.stopCollecting()
                        showResults = true
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val data by viewModel.data.collectAsState()
                        val error by viewModel.error.collectAsState()

                        error?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (!showResults) {
                            Text(
                                text = "Workout in Progress: $timeRemaining s",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Stage.values().forEach { stage ->
                                    Text(
                                        text = stage.name,
                                        color = if (stage == data.currentStage)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Steps: ${data.steps}")
                                    Text("Distance: %.2f m".format(data.distance))
                                    Text("Pace: %.1f min/km".format(data.pace))
                                    Text("Calories: %.1f".format(data.calories))
                                    Text("Level: ${data.fitnessLevel}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}