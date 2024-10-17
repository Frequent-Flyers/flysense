package com.example.airsense

import com.example.airsense.detector.algorithm.SensorType
import com.example.airsense.detector.sensors.MeasurableSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.sqrt

@HiltViewModel
class SimulationViewModel @Inject constructor(
    @Named("simulatedAccelerometerSensor") private var simulatedAccelerometerSensor: MeasurableSensor,

//    @Named("simulatedOrientationSensor") private var simulatedOrientationSensor: MeasurableSensor,

    @Named("simulatedBarometerSensor") private var simulatedBarometerSensor: MeasurableSensor
) : BaseViewModel() {
    var timeElapsed = 0L

    // Simulation-specific logic goes here
    init {
        // Start simulated sensors and use shared methods from BaseViewModel
        startSimulatedAccelerometerSensor()
//        startSimulatedOrientationSensor()
        startSimulatedBarometerSensor()
    }

    private fun startSimulatedAccelerometerSensor() {
        simulatedAccelerometerSensor.startListening()
        simulatedAccelerometerSensor.setOnSensorValuesChangedListener { values ->
            if (accelFirstTimestamp == 0L) {
                accelFirstTimestamp = values[0].toLong()
            }
            val timestamp = values[0].toLong()

            // Remaining values are the x, y, z sensor readings
            val x = values[1]
            val y = values[2]
            val z = values[3]

            // Calculate acceleration
            val acceleration = sqrt(x * x + y * y + z * z.toDouble()).toFloat()
            absoluteAcceleration = acceleration

            flightDetectionAlgorithm.onSensorData(SensorType.ACCELEROMETER, values)
            //Log.d("her er x", x.toString())
            // Calculate the time between points
            lastTimestamp = accelCurrentTimestamp
            accelCurrentTimestamp = timestamp
            if (lastTimestamp != 0L) {
                timeBetweenPoints = accelCurrentTimestamp - lastTimestamp
            }
        }
    }

//    private fun startSimulatedOrientationSensor() {
//        simulatedOrientationSensor.startListening()
//        simulatedOrientationSensor.setOnSensorValuesChangedListener { values ->
//            val timestamp = values[0].toLong()
//
//            // Remaining values are the yaw, qx, qz, roll, qw, qy, pitch sensor readings
//            var x = values[1].toFloat()
//            var y = values[2].toFloat()
//            var z = values[3].toFloat()
//            yaw = x
//            roll = y
//            pitch = z
//            flightDetectionAlgorithm.onSensorData(SensorType.ORIENTATION, values)
//
//            // Temporary string formatting to limit the number of decimal places
//            yaw = String.format("%.2f", yaw).toFloat()
//            roll = String.format("%.2f", roll).toFloat()
//            pitch = String.format("%.2f", pitch).toFloat()
//        }
//    }

    private fun startSimulatedBarometerSensor() {
        simulatedBarometerSensor.startListening()
        simulatedBarometerSensor.setOnSensorValuesChangedListener { values ->
            if (baroFirstTimestamp == 0L) {
                baroFirstTimestamp = values[0].toLong()
            }
            // First value is the timestamp
            val timestamp = values[0].toLong()
            pressure = values[1].toFloat()

            flightDetectionAlgorithm.onSensorData(SensorType.BAROMETER, values)
            baroCurrentTimestamp = timestamp
        }
    }

    fun setSimulatedData(dataStreams: Map<CSVDataLoader.DataType, MutableList<List<Pair<Long, DoubleArray>>>>) {
//        dataStreams = data
//        dataStreams?.let { streams ->
//            streams[CSVDataLoader.DataType.ACCELEROMETER]?.forEach {
//                simulatedAccelerometerSensor.loadData(it)
//            }
//            streams[CSVDataLoader.DataType.ORIENTATION]?.forEach {
//                simulatedOrientationSensor.loadData(it)
//            }
//            streams[CSVDataLoader.DataType.BAROMETER]?.forEach {
//                simulatedBarometerSensor.loadData(it)
//            }
//        }
        dataStreams.forEach { mapEntry ->
            val dataType = mapEntry.key
            val data = mapEntry.value

            when (dataType) {
                CSVDataLoader.DataType.ACCELEROMETER -> {
                    simulatedAccelerometerSensor.loadData(data.flatten())
                }

//                CSVDataLoader.DataType.ORIENTATION -> {
//                    simulatedOrientationSensor.loadData(data.flatten())
//                }

                CSVDataLoader.DataType.BAROMETER -> {
                    simulatedBarometerSensor.loadData(data.flatten())
                }

                CSVDataLoader.DataType.UNKNOWN -> {
                    // Do nothing
                }
            }
        }
    }

    fun stopSimulatedSensors() {
        // Stop the simulated accelerometer sensor
        simulatedAccelerometerSensor.stopListening()

        // Stop the simulated orientation sensor
//        simulatedOrientationSensor.stopListening()

        // Stop the simulated barometer sensor
        simulatedBarometerSensor.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulatedSensors() // This will cancel ongoing coroutines
    }
}