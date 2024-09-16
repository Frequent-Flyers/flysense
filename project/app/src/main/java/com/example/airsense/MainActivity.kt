package com.example.airsense

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
    private var latestSensorEvent by mutableStateOf<SensorEvent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val dataFlow = parse(this, "Accelerometer", "Accelerometer.csv")

        // Create the simulator and pass this activity's SensorEventListener
        simulator = Simulator(
            sensorManager,
            this,
            Sensor.TYPE_ACCELEROMETER,
            dataFlow,
            resampleRate = 1000000
        )

        // Start the simulation with the CSV file
        simulator.runSimulation()

        setContent {
            AirsenseTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android")
                    Test(this, "Accelerometer.csv")
                    latestSensorEvent?.let { event ->
                        DisplaySimulatedData(event)
                    }
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        latestSensorEvent = event
        // This method will now receive simulated data
        Log.d(
            "SensorSimulator",
            "Received simulated sensor data: ${event.values.contentToString()}"
        )
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
fun SensorDataDisplay(sensorData: SensorData) {
    Column {
        Text(text = "Simulated Sensor Data:")
        Text(text = "X: ${sensorData.getX()}")
        Text(text = "Y: ${sensorData.getY()}")
        Text(text = "Z: ${sensorData.getZ()}")
    }
}

// Inside your MainActivity or wherever you're injecting the data
@Composable
fun DisplaySimulatedData(event: SensorEvent) {
    val sensorData = SensorData(
        "acc",
        event.timestamp.toString(),
        5.53.toFloat(),
        event.values[0],
        event.values[1],
        event.values[2]
    )
    SensorDataDisplay(sensorData)
}