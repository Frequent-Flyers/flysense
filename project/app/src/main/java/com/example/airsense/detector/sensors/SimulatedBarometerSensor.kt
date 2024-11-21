package com.example.airsense.detector.sensors

import android.hardware.Sensor
import android.util.Log
import com.example.airsense.CSVDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class SimulatedBarometerSensor(
    private val csvDataLoader: CSVDataLoader,
    private val speed: Double
) : MeasurableSensor(Sensor.TYPE_PRESSURE) {

    private var sensorData: List<Pair<Long, DoubleArray>> = csvDataLoader.loadData()
    private var sensorJob: Job? = null
    private var coefficient = 1.0 / speed

    override val doesSensorExist: Boolean
        get() = true // Always return true for the fake sensor

    override fun startListening() {
        sensorJob?.cancel() // Cancel any existing job if it's running

        if (sensorData.isEmpty()) {
            return
        }
        sensorJob = CoroutineScope(Dispatchers.Default).launch {
            for ((timestamp, data) in sensorData) {
                // Use the listener set in MeasurableSensor
                onSensorValuesChanged?.invoke(listOf(timestamp.toDouble()) + data.toList())
                delay((1000L * coefficient).roundToLong()) // Simulate the sensor delay (e.g., 1 second)
                //delay using thread.wait() to simulate the sensor delay
            }
        }
        Log.d("SimulatedBarometerSensor", "Sensor data has been exhausted")
    }

    override fun stopListening() {
        sensorJob?.cancel() // Stop the sensor job when the fake sensor is stopped
        sensorData = emptyList()
    }

    override fun loadData(it: List<Pair<Long, DoubleArray>>) {
        this.sensorData = it
        startListening()
    }
}
