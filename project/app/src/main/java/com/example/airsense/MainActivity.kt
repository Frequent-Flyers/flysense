package com.example.airsense

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airsense.ui.theme.AirsenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirsenseTheme {
                val viewModel = viewModel<MainViewModel>()
                val useFakeSensor = viewModel.useFakeSensor

                // Using a Column to arrange elements vertically
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center, // Centers the content vertically
                    horizontalAlignment = Alignment.CenterHorizontally // Centers content horizontally
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Absolute acceleration: ${viewModel.absoluteAcceleration}\n" +
                                    "Current timestamp: ${viewModel.currentTimestamp}\n" +
                                    "Time between points: ${viewModel.timeBetweenPoints}",
                        )
                    }

                    //for orientation sensor
                    Spacer(modifier = Modifier.height(16.dp)) // Add vertical space of 16.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pitch: ${viewModel.pitch}\n" +
                                    "Roll: ${viewModel.roll}\n" +
                                    "Yaw: ${viewModel.yaw}",
                        )
                    }

                    //for orientation sensor
                    Spacer(modifier = Modifier.height(16.dp)) // Add vertical space of 16.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pressure: ${viewModel.pressure}\n",
                        )
                    }

                    // Spacer
                    Spacer(modifier = Modifier.height(16.dp))

                    // Button to toggle between real and fake accelerometer data
                    Button(
                        onClick = {
                            viewModel.toggleSensor()
                        }
                    ) {
                        Text(text = if (useFakeSensor) "Use Real Sensor" else "Use Simulated Sensor")
                    }

                    EnterSimulator(viewModel)
                }
            }
        }
    }
}

@Composable
fun EnterSimulator(viewModel: MainViewModel) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
    }

    Column {
        Button(onClick = {
            val intent = Intent(context, SimulatorActivity::class.java)
            launcher.launch(intent)
        }) {
            Text(text = "Enter Simulator")
        }
    }
}