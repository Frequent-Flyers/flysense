package com.example.airsense.detector.algorithm

import android.util.Log

class FlightDetectionAlgorithm {
    private val maxAccelDataPoints = 15000 // 150 seconds at 100Hz
    private val maxBaroDataPoints = 150 // 150 seconds at 1Hz
    private val carryOverPoints = 5000
    private val sensorDataQueues = mapOf(
        SensorType.ACCELEROMETER to ArrayDeque<List<Double>>(maxAccelDataPoints),
        SensorType.BAROMETER to ArrayDeque<List<Double>>(maxBaroDataPoints),
        SensorType.ORIENTATION to ArrayDeque<List<Double>>(maxAccelDataPoints)
    )

    private var accelDataCounter = 0
    private var lastFlightState = false
    private var flightStartTime = 0.0
    private var counter = 0
    var firstTimeStamp = 0.0

    fun reset() {
        sensorDataQueues.values.forEach { it.clear() }
        accelDataCounter = 0
        lastFlightState = false
        flightStartTime = 0.0
    }

    fun onSensorData(sensorType: SensorType, data: List<Double>) {
        if (data.isEmpty()) {
            Log.e("FlightDetectionAlgorithm", "Received empty sensor data for $sensorType")
            return
        }
        if (firstTimeStamp == 0.0) {
            firstTimeStamp = data[0]
        }

        sensorDataQueues[sensorType]?.let { queue ->
            val maxSize =
                if (sensorType == SensorType.BAROMETER) maxBaroDataPoints else maxAccelDataPoints
            if (queue.size >= maxSize) queue.removeFirst()
            queue.addLast(data)
        }

        if (sensorType == SensorType.ACCELEROMETER) {
            accelDataCounter++
            if (accelDataCounter >= maxAccelDataPoints - carryOverPoints) {
                detectFlight()
                trimQueueToCarryover(sensorDataQueues[SensorType.ACCELEROMETER]!!, carryOverPoints)
                trimQueueToCarryover(sensorDataQueues[SensorType.ORIENTATION]!!, carryOverPoints)
                accelDataCounter = carryOverPoints
            }
        }
    }

    private fun trimQueueToCarryover(queue: ArrayDeque<List<Double>>, carryOver: Int) {
        while (queue.size > carryOver) queue.removeFirst()
    }

    private fun detectFlight() {
        val accelerometerData = sensorDataQueues[SensorType.ACCELEROMETER] ?: return
        val barometerData = sensorDataQueues[SensorType.BAROMETER] ?: return

        if (accelerometerData.size < carryOverPoints || barometerData.isEmpty()) {
            Log.e("FlightDetectionAlgorithm", "Insufficient data for detection")
            return
        }

        val timestamps = accelerometerData.map { it[0] }
        val absoluteAcceleration = accelerometerData.map {
            val (_, x, y, z) = it
            Math.sqrt(x * x + y * y + z * z)
        }

        val smoothedAcceleration = movingAverage(absoluteAcceleration, 997)
        val varianceAcceleration = variance(absoluteAcceleration, 997)

        // Interpolate barometer data to match accelerometer timestamps
        //val interpolatedBarometerData = interpolateBarometerData(barometerData.toList(), timestamps)
        val interpolatedBarometerData = barometerData.map { it[1] }

        val isInFlight = detectTakeoffState(
            timestamps,
            smoothedAcceleration,
            varianceAcceleration,
            interpolatedBarometerData
        )

        if (isInFlight != lastFlightState) {
            if (isInFlight) {
                flightStartTime = timestamps.last()
                Log.d("FlightDetectionAlgorithm", "Flight detected, started at $flightStartTime")
            } else {
                val flightDuration = timestamps.last() - flightStartTime
                Log.d("FlightDetectionAlgorithm", "Flight ended. Duration: $flightDuration seconds")
            }
            lastFlightState = isInFlight
        }
    }

    private fun interpolateBarometerData(
        barometerData: List<List<Double>>,
        targetTimestamps: List<Double>
    ): List<Double> {
        if (barometerData.size < 2) return List(targetTimestamps.size) {
            barometerData.firstOrNull()?.get(1) ?: 0.0
        }

        return targetTimestamps.map { timestamp ->
            val (lowerIndex, upperIndex) = findNearestIndices(barometerData, timestamp)
            val (lowerTime, lowerPressure) = barometerData[lowerIndex]
            val (upperTime, upperPressure) = barometerData[upperIndex]

            if (lowerTime == upperTime) lowerPressure
            else {
                val ratio = (timestamp - lowerTime) / (upperTime - lowerTime)
                lowerPressure + ratio * (upperPressure - lowerPressure)
            }
        }
    }

    private fun findNearestIndices(data: List<List<Double>>, timestamp: Double): Pair<Int, Int> {
        val index = data.binarySearch { it[0].compareTo(timestamp) }
        return if (index >= 0) Pair(index, index)
        else {
            val insertionPoint = -(index + 1)
            Pair(maxOf(0, insertionPoint - 1), minOf(data.lastIndex, insertionPoint))
        }
    }

    private fun detectTakeoffState(
        timestamps: List<Double>,
        smoothedAcceleration: List<Double>,
        varianceAcceleration: List<Double>,
        barometerData: List<Double>
    ): Boolean {
        counter++
        //Log.d("FlightDetectionAlgorithm", "Counter: $counter")
        val takeoffAccelerationThreshold = 0.45..0.8
        val takeoffDuration = 25.0 // seconds
        var takeoffStartTime = 0.0
        var inTakeoff = false
        var currentMaxVariance = 0.0
        var validTakeoffSpotFound = false
        var peakStartTime = 0.0
        var highVarianceFound = false
        var finalTime = 0.0
        var endTime = 0.0

        // Convert nanoseconds to seconds for all timestamps
        val timestampsInSeconds = timestamps.map { it / 1_000_000_000.0 }

        for (i in smoothedAcceleration.indices) {
            var currentAcceleration = smoothedAcceleration[i]
            var currentTime = timestampsInSeconds[i] // Now in seconds

            if (!validTakeoffSpotFound) {
                if (currentAcceleration in takeoffAccelerationThreshold) {
                    if (!inTakeoff) {
                        inTakeoff = true
                        takeoffStartTime = currentTime
                        currentMaxVariance = 0.0
//                    Log.d(
//                        "TakeoffDetection",
//                        "Potential takeoff start at $currentTime seconds, acceleration: $currentAcceleration"
//                    )
                    }

                    if (varianceAcceleration[i] > currentMaxVariance) {
                        currentMaxVariance = varianceAcceleration[i]
                    }

                    if ((currentTime - takeoffStartTime) >= takeoffDuration) {
                        inTakeoff = false
                        endTime = currentTime
                        if (currentMaxVariance < 1.0) {
                            validTakeoffSpotFound = true
                            Log.d(
                                "TakeoffDetection",
                                "Valid takeoff spot found. Duration: ${currentTime - takeoffStartTime} seconds, Max Variance: $currentMaxVariance"
                            )
                            currentMaxVariance = 0.0
                        }
                    }
                } else {
                    inTakeoff = false
                    currentMaxVariance = 0.0
                }
            } else {
//                Log.d("TakeoffDetection", "Checking for acceleration peak after takeoff spot")
                // Check for acceleration peak within 30 seconds after takeoff spot
                val peakEndTime = takeoffStartTime + 30.0 // seconds

                currentTime = timestampsInSeconds[i] // Now in seconds
//                if (currentTime > peakEndTime) {
//                    break
//                }

                if (currentTime - endTime <= 30) {
                    currentAcceleration = smoothedAcceleration[i]
                    Log.d(
                        "TakeoffDetection",
                        "Checking for acceleration peak at ${currentTime - (firstTimeStamp / 1000000000)} seconds, acceleration: $currentAcceleration"
                    )
                    if (currentAcceleration in 1.5..2.5) {
                        if (peakStartTime == 0.0) {
                            peakStartTime = currentTime
                            Log.d(
                                "TakeoffDetection",
                                "Potential acceleration peak start at $currentTime seconds, acceleration: $currentAcceleration"
                            )
                        } else {
                            Log.d(
                                "yoiyo",
                                "inside acceleration peak start at $currentTime seconds, acceleration: $currentAcceleration"
                            )
                            // Check for high variance in the next 30 seconds
                            if ((currentTime - peakStartTime) >= 5.0) {
                                val varianceCheckEndTime = currentTime + 30.0
                                finalTime = currentTime

                                for (j in i until smoothedAcceleration.size) {
                                    if (timestampsInSeconds[j] > varianceCheckEndTime) break
                                    if (varianceAcceleration[j] > 6.0) {
                                        highVarianceFound = true
                                        Log.d(
                                            "TakeoffDetection",
                                            "High variance found at ${timestampsInSeconds[j]} seconds, variance: ${varianceAcceleration[j]}"
                                        )
                                        break
                                    }
                                }

                            }
                            if (!highVarianceFound) {
                                Log.d("FlightDetectionAlgorithm", "Takeoff detected")
                                return true // Takeoff detected
                            } else {
                                Log.d(
                                    "TakeoffDetection",
                                    "Takeoff not confirmed due to high variance after acceleration peak."
                                )
                            }
                            validTakeoffSpotFound = false
                            peakStartTime = 0.0
                        }
                    } else {
                        peakStartTime = 0.0
                    }
                } else {
                    validTakeoffSpotFound = false
                    peakStartTime = 0.0
                }
            }
        }

        return false
    }

    private fun movingAverage(data: List<Double>, windowSize: Int): List<Double> {
        return data.windowed(windowSize, 1, true) { it.average() }
    }

    private fun variance(data: List<Double>, windowSize: Int): List<Double> {
        return data.windowed(windowSize, 1, true) { window ->
            val mean = window.average()
            window.map { (it - mean) * (it - mean) }.average()
        }
    }
}

enum class SensorType {
    ACCELEROMETER, BAROMETER, ORIENTATION
}