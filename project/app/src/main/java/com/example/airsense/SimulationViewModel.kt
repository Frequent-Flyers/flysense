package com.example.airsense

import android.util.Log
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
        startSimulatedSensors()
    }

    private fun startSimulatedSensors() {
        val accelerometerQueue = ArrayDeque<List<Double>>()
        val barometerQueue = ArrayDeque<List<Double>>()

        var isProcessing = false

        simulatedAccelerometerSensor.startListening()
        simulatedBarometerSensor.startListening()

        simulatedAccelerometerSensor.setOnSensorValuesChangedListener { accelValues ->
            if (accelFirstTimestamp == 0L) {
                accelFirstTimestamp = accelValues[0].toLong()
                Log.d("SimulatedAccelerometerSensor", "First accel timestamp: $accelFirstTimestamp")
            }
            // Calculate the acceleration
            val timestamp = accelValues[0].toLong()
            val x = accelValues[1]
            val y = accelValues[2]
            val z = accelValues[3]
            val acceleration = sqrt(x * x + y * y + z * z.toDouble()).toFloat()
            absoluteAcceleration = acceleration
            lastTimestamp = accelCurrentTimestamp
            accelCurrentTimestamp = timestamp
            if (lastTimestamp != 0L) {
                timeBetweenPoints = accelCurrentTimestamp - lastTimestamp
            }
            // Add accelerometer data to the queue
            accelerometerQueue.add(accelValues)
            // Check if both queues have sufficient data
            if (!isProcessing && accelerometerQueue.size >= 7000 && barometerQueue.size >= 7000) {
                isProcessing = true
                processNextBatch(accelerometerQueue, barometerQueue)
                isProcessing = false
            }

        }
        simulatedBarometerSensor.setOnSensorValuesChangedListener { baroValues ->
            if (baroFirstTimestamp == 0L) {
                baroFirstTimestamp = baroValues[0].toLong()
                Log.d("SimulatedBarometerSensor", "First baro timestamp: $baroFirstTimestamp")
            }
            // First value is the timestamp
            val timestamp = baroValues[0].toLong()
            pressure = baroValues[1].toFloat()

            baroCurrentTimestamp = timestamp

            while (barometerQueue.size > accelerometerQueue.size) {
                Thread.sleep(1) // Simple throttle to ensure synchronization
            }

            barometerQueue.add(baroValues)
        }

    }

    private fun processNextBatch(
        accelerometerQueue: ArrayDeque<List<Double>>,
        barometerQueue: ArrayDeque<List<Double>>
    ) {
        // Take the first 15k points from both queues
        val accelerometerData = accelerometerQueue.take(7000)
        val barometerData = barometerQueue.take(7000)

        // Send data to the flight detection algorithm
        processCombinedData(accelerometerData, barometerData)

        // Remove the oldest 10k points, leaving a carryover of 5k
        trimQueue(accelerometerQueue, 4000)
        trimQueue(barometerQueue, 4000)
    }

    // Function to process combined data
    private fun processCombinedData(
        accelerometerData: List<List<Double>>,
        barometerData: List<List<Double>>
    ) {
        flightDetectionAlgorithm.processData(accelerometerData, barometerData)
    }

    // Function to trim the queue to retain the latest 'retainSize' elements
    private fun <T> trimQueue(queue: ArrayDeque<T>, retainSize: Int) {
        while (queue.size > retainSize) {
            queue.removeFirst()
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
            Log.d("SimulatorActivity", "Data size: ${data[0].size} for type: $dataType")

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