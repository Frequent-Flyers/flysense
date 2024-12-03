package com.example.airsense

import android.util.Log
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import com.example.airsense.detector.sensors.MeasurableSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt
import kotlin.math.sqrt

@HiltViewModel
class SimulationViewModel @Inject constructor(
    @Named("simulatedAccelerometerSensor") private var simulatedAccelerometerSensor: MeasurableSensor,
    @Named("simulatedBarometerSensor") private var simulatedBarometerSensor: MeasurableSensor
) : BaseViewModel() {
    var timeElapsed = 0L
    private val flightDetectionAlgorithm = FlightDetectionAlgorithm()
    private val defaultBatchSize = 7000
    private val defaultCarryOver = 4000

    private var _frequency = 0.0
    var frequency: Double
        get() = _frequency
        set(value) {
            _frequency = value
            adjustBatchSizeAndCarryOver() // Recalculate batchSize and carryOver
        }

    private var batchSize = defaultBatchSize
    private var carryOver = defaultCarryOver

    // Simulation-specific logic goes here
    init {
        // Start simulated sensors and use shared methods from BaseViewModel
        flightDetectionAlgorithm.reset()
        startSimulatedSensors()
    }

    private fun adjustBatchSizeAndCarryOver() {
        if (frequency > 0) {
            //round frequency up to nearest integer
            var roundedFreq = frequency.roundToInt().toDouble()
            var adjustment = roundedFreq / 100.0
            batchSize = (defaultBatchSize * adjustment).toInt()
            carryOver = (defaultCarryOver * adjustment).toInt()
            Log.d("SimulationViewModel", "Adjusted batchSize: $batchSize, carryOver: $carryOver")
            flightDetectionAlgorithm.adjustFrequency(roundedFreq.toInt())
        }
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
            synchronized(accelerometerQueue) {
                accelerometerQueue.add(accelValues)
            }
            // Check if both queues have sufficient data
            synchronized(this) {
                if (!isProcessing && accelerometerQueue.size >= batchSize && barometerQueue.size >= batchSize) {
                    isProcessing = true
                    processNextBatch(
                        accelerometerQueue.take(batchSize),
                        barometerQueue.take(batchSize)
                    ) // Only send batches
                    removeOldestData(
                        accelerometerQueue,
                        batchSize - carryOver
                    ) // Remove 3k oldest points
                    removeOldestData(
                        barometerQueue,
                        batchSize - carryOver
                    ) // Remove 3k oldest points
                    isProcessing = false
                }
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

            synchronized(barometerQueue) {
                barometerQueue.add(baroValues)
            }
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

    private fun <T> removeOldestData(queue: ArrayDeque<T>, removeCount: Int) {
        val originalSize = queue.size
        synchronized(queue) {
            repeat(removeCount.coerceAtMost(queue.size)) { // Avoid removing more than the queue size
                queue.removeFirst()
            }
        }
//        Log.d("SimulationViewModel", "Trimmed queue from $originalSize to ${queue.size}")
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