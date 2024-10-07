package com.example.airsense.detector.sensors

import android.hardware.Sensor
import com.example.airsense.CSVDataLoader
import kotlinx.coroutines.*

class SimulatedOrientationSensor(
    private val csvDataLoader: CSVDataLoader
) : MeasurableSensor(Sensor.TYPE_ROTATION_VECTOR) {

    private var sensorData: List<Pair<Long, DoubleArray>> = csvDataLoader.loadData()
    private var sensorJob: Job? = null

    override val doesSensorExist: Boolean
        get() = true // Always return true for the fake sensor

    override fun startListening() {
        sensorJob?.cancel() // Cancel any existing job if it's running
        sensorJob = CoroutineScope(Dispatchers.Default).launch {
            for ((timestamp, data) in sensorData) {
                // Use the listener set in MeasurableSensor
                onSensorValuesChanged?.invoke(listOf(timestamp.toDouble()) + data.toList())
                delay(10) // Simulate the sensor delay (e.g., 1 second)
            }
        }
    }

    override fun stopListening() {
        sensorJob?.cancel() // Stop the sensor job when the fake sensor is stopped
    }

    override fun loadData(it: List<Pair<Long, DoubleArray>>) {
        this.sensorData = it
    }
}
