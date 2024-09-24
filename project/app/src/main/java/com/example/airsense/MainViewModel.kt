package com.example.airsense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.sqrt

@HiltViewModel
class MainViewModel @Inject constructor(
    @Named("lightSensor") private val lightSensor: MeasurableSensor,

    @Named("realAccelerometerSensor") private val realAccelerometerSensor: MeasurableSensor,
    @Named("simulatedAccelerometerSensor") private val simulatedAccelerometerSensor: MeasurableSensor,

    @Named("realOrientationSensor") private val realOrientationSensor: MeasurableSensor,
    @Named("simulatedOrientationSensor") private val simulatedOrientationSensor: MeasurableSensor,

    @Named("realBarometerSensor") private val realBarometerSensor: MeasurableSensor,
    @Named("simulatedBarometerSensor") private val simulatedBarometerSensor: MeasurableSensor
) : ViewModel() {

    var isDark by mutableStateOf(false)
    var lightValue by mutableStateOf(0f)

    var absoluteAcceleration by mutableStateOf(0f)
    var useFakeSensor by mutableStateOf(false)

    var currentTimestamp by mutableStateOf(0L)
    var lastTimestamp by mutableStateOf(0L)
    var timeBetweenPoints by mutableStateOf(0L)

    var pitch by mutableStateOf(0f)
    var roll by mutableStateOf(0f)
    var yaw by mutableStateOf(0f)

    var pressure by mutableStateOf(0f)

    private lateinit var currentAccelerometerSensor: MeasurableSensor
    private lateinit var currentOrientationSensor: MeasurableSensor
    private lateinit var currentBarometerSensor: MeasurableSensor

    init {
        // Light sensor logic
        lightSensor.startListening()
        lightSensor.setOnSensorValuesChangedListener { values ->
            val lux = values[1]
            lightValue = lux.toFloat()
            isDark = lux < 60f
        }

        // Start with the real accelerometer sensor by default
        currentAccelerometerSensor = realAccelerometerSensor
        currentOrientationSensor = realOrientationSensor
        currentBarometerSensor = realBarometerSensor
        startAccelerometerSensor()
        startOrientationSensor()
        startBarometerSensor()
    }

    private fun startAccelerometerSensor() {
        currentAccelerometerSensor.startListening()
        currentAccelerometerSensor.setOnSensorValuesChangedListener { values ->
            // First value is the timestamp
            val timestamp = values[0].toLong()

            // Remaining values are the x, y, z sensor readings
            val x = values[1]
            val y = values[2]
            val z = values[3]

            // Calculate acceleration
            val acceleration = sqrt(x * x + y * y + z * z.toDouble()).toFloat()
            absoluteAcceleration = acceleration

            // Calculate the time between points
            lastTimestamp = currentTimestamp
            currentTimestamp = timestamp
            if (lastTimestamp != 0L) {
                timeBetweenPoints = currentTimestamp - lastTimestamp
            }
        }
    }

    fun toggleSensor() {
        // Stop the current sensor
        currentAccelerometerSensor.stopListening()
        currentOrientationSensor.stopListening()
        currentBarometerSensor.stopListening()

        // Toggle between real and fake sensors
        useFakeSensor = !useFakeSensor
        currentAccelerometerSensor = if (useFakeSensor) {
            simulatedAccelerometerSensor
        } else {
            realAccelerometerSensor
        }

        currentOrientationSensor = if (useFakeSensor) {
            simulatedOrientationSensor
        } else {
            realOrientationSensor
        }

        currentBarometerSensor = if (useFakeSensor) {
            simulatedBarometerSensor
        } else {
            realBarometerSensor
        }

        // Start the new sensor
        startAccelerometerSensor()
        startOrientationSensor()
        startBarometerSensor()
    }

    private fun startOrientationSensor() {
        currentOrientationSensor.startListening()
        currentOrientationSensor.setOnSensorValuesChangedListener { values ->
            // First value is the timestamp
            val timestamp = values[0].toLong()

            // Remaining values are the yaw, qx, qz, roll, qw, qy, pitch sensor readings
            var x = values[1].toFloat()
            var y = values[2].toFloat()
            var z = values[3].toFloat()

            if (currentOrientationSensor == realOrientationSensor) {
                var scalar = values[4].toFloat()
                var headingAcc = values[5].toFloat()

                // Correct calculation of pitch, roll, and yaw from the quaternion values
                roll = Math.asin((2 * (scalar * y - z * x)).toDouble())
                    .toFloat() * (180 / Math.PI.toFloat())  // Rotation around X-axis
                pitch = Math.atan2(
                    (2 * (scalar * x + y * z)).toDouble(),
                    (1 - 2 * (x * x + y * y)).toDouble()
                ).toFloat() * (180 / Math.PI.toFloat())  // Rotation around Y-axis
                yaw = Math.atan2(
                    (2 * (scalar * z + x * y)).toDouble(),
                    (1 - 2 * (y * y + z * z)).toDouble()
                ).toFloat() * (180 / Math.PI.toFloat())  // Rotation around Z-axis
            } else {
                yaw = x
                roll = y
                pitch = z
            }

        }
    }

    private fun startBarometerSensor() {
        currentBarometerSensor.startListening()
        currentBarometerSensor.setOnSensorValuesChangedListener { values ->
            // First value is the timestamp
            val timestamp = values[0].toLong()
            pressure = values[1].toFloat()
        }
    }

    override fun onCleared() {
        super.onCleared()
        lightSensor.stopListening()
        currentAccelerometerSensor.stopListening()
        currentOrientationSensor.stopListening()
    }
}