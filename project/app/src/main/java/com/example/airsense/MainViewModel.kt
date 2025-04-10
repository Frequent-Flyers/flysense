package com.example.airsense

import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import com.example.airsense.detector.algorithm.FlightState
import com.example.airsense.detector.sensors.MeasurableSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.sqrt

@HiltViewModel
class MainViewModel @Inject constructor(
    @Named("realAccelerometerSensor") private val realAccelerometerSensor: MeasurableSensor,
    @Named("realBarometerSensor") private val realBarometerSensor: MeasurableSensor,
    val flightDetectionAlgorithm: FlightDetectionAlgorithm
) : BaseViewModel() {


    private lateinit var currentAccelerometerSensor: MeasurableSensor
    private lateinit var currentBarometerSensor: MeasurableSensor
    private var accelQueue = ArrayDeque<List<Double>>()
    private var baroQueue = ArrayDeque<List<Double>>()
    private val batchSize = 8000
    private val carryOver = 6000
    var isProcessing = false

    private val _flightState = MutableStateFlow(FlightState.GROUNDED)
    val flightState: StateFlow<FlightState> = _flightState.asStateFlow()

//    private val flightDetectionAlgorithm = FlightDetectionAlgorithm()

    init {
        // Start with the real accelerometer sensor by default
        currentAccelerometerSensor = realAccelerometerSensor
        currentBarometerSensor = realBarometerSensor
        startAccelerometerSensor()
        startBarometerSensor()
        //flightDetectionAlgorithm.adjustFrequency(50)

        //flightDetectionAlgorithm.listener = { newState ->
        //    _flightState.value = newState
        //}

        flightDetectionAlgorithm.addListener { newState ->
            _flightState.value = newState
        }
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

//            flightDetectionAlgorithm.onSensorData(SensorType.ACCELEROMETER, values)

            // Calculate the time between points
            lastTimestamp = accelCurrentTimestamp
            accelCurrentTimestamp = timestamp
            if (lastTimestamp != 0L) {
                timeBetweenPoints = accelCurrentTimestamp - lastTimestamp
            }
            accelQueue.add(values)

            if (!isProcessing && accelQueue.size >= batchSize && baroQueue.size >= batchSize) {
                isProcessing = true
                processNextBatch(
                    accelQueue.toList(),
                    baroQueue.toList()
                ) // Only send batches
                removePercentageOldestData(accelQueue, 0.25)
                removePercentageOldestData(baroQueue, 0.25)
                isProcessing = false
            }
        }
    }


    private fun startBarometerSensor() {
        currentBarometerSensor.startListening()
        currentBarometerSensor.setOnSensorValuesChangedListener { values ->
            // First value is the timestamp
            val timestamp = values[0].toLong()
            pressure = values[1].toFloat()

//            flightDetectionAlgorithm.onSensorData(SensorType.BAROMETER, values)

            baroQueue.add(values)
        }
    }

    private fun processNextBatch(
        accelBatch: List<List<Double>>,
        baroBatch: List<List<Double>>
    ) {
        // Send the 7k points to the algorithm
        processCombinedData(accelBatch, baroBatch)
    }

    // Function to process combined data
    private fun processCombinedData(
        accelerometerData: List<List<Double>>,
        barometerData: List<List<Double>>
    ) {
        flightDetectionAlgorithm.processData(accelerometerData, barometerData)
    }

    private fun <T> removePercentageOldestData(queue: ArrayDeque<T>, percentage: Double) {
        synchronized(queue) {
            val removeCount = (queue.size * percentage).toInt()
            repeat(removeCount.coerceAtMost(queue.size)) { // Avoid removing more than the queue size
                queue.removeFirst()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentAccelerometerSensor.stopListening()
        currentBarometerSensor.stopListening()
    }

    fun stopListening() {
        currentAccelerometerSensor.stopListening()
        currentBarometerSensor.stopListening()
    }

    fun startListening() {
        currentAccelerometerSensor.startListening()
        currentBarometerSensor.startListening()
    }
}