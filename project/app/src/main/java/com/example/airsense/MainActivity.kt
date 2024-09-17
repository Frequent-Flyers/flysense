package com.example.airsense

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.airsense.ui.theme.AirsenseTheme

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var simulator: Simulator

    private var latestAccelerometerEvent by mutableStateOf<SensorEvent?>(null)
    private var latestBarometerEvent by mutableStateOf<SensorEvent?>(null)
    private var latestOrientationEvent by mutableStateOf<SensorEvent?>(null)

    private var isSimulationRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
//        val dataFlow = parse(this, "Accelerometer", "Accelerometer.csv")

        setContent {
            AirsenseTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
//                    Greeting("Android")
//                    Test(this, "Accelerometer.csv")
                    Column {
                        // Display data for different sensors
                        latestAccelerometerEvent?.let { DisplaySensorData("Accelerometer", it) }
                        latestBarometerEvent?.let { DisplaySensorData("Barometer", it) }
                        latestOrientationEvent?.let { DisplaySensorData("Orientation", it) }

                        // Button to toggle simulation
                        Button(onClick = {
                            if (isSimulationRunning) {
                                stopSimulation()
                            } else {
                                startSimulation()
                            }
                        }) {
                            Text(if (isSimulationRunning) "Stop Simulation" else "Start Simulation")
                        }
                    }
                }
            }
        }
    }

    private fun startSimulation() {
        isSimulationRunning = true
        val accelerometerData = parse(this, "Accelerometer", "Accelerometer.csv")
        val barometerData = parse(this, "Barometer", "Barometer.csv")
        val orientationData = parse(this, "Orientation", "Orientation.csv")

        simulator = Simulator(
            sensorManager,
            this,
            listOf(
                Sensor.TYPE_ACCELEROMETER to accelerometerData,
                Sensor.TYPE_PRESSURE to barometerData,
                Sensor.TYPE_ORIENTATION to orientationData
            ),
            resampleRate = 1000000
        )
        simulator.runSimulation()
    }

    // Stop the simulation
    private fun stopSimulation() {
        isSimulationRunning = false
        simulator.stopSimulation()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isSimulationRunning) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> latestAccelerometerEvent = event
                Sensor.TYPE_PRESSURE -> latestBarometerEvent = event
                Sensor.TYPE_ORIENTATION -> latestOrientationEvent = event
            }
        }
        // This method will now receive simulated data
//        Log.d(
//            "SensorSimulator",
//            "Received simulated sensor data: ${event.values.contentToString()}"
//        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this simulation
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop simulation when activity is destroyed
        simulator.stopSimulation()
    }
}

@Composable
fun Greeting(name: String) {
//    Text(text = "Hello $name!")
}

@Composable
fun Test(context: Context, fileName: String) {
    val dataFlow = parse(context, "Accelerometer", fileName)
    Text(text = "\n")
    Text(text = dataFlow[1].getX().toString())
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AirsenseTheme {
        Greeting("Android")
    }
}

@Composable
fun DisplaySensorData(sensorName: String, event: SensorEvent) {
    Column {
        Text(text = "$sensorName Data:")
        Text(text = "X: ${event.values.getOrNull(0)}")
        Text(text = "Y: ${event.values.getOrNull(1)}")
        Text(text = "Z: ${event.values.getOrNull(2)}")
        Text(text = "Timestamp: ${event.timestamp}")
    }
}