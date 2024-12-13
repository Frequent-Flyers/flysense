package com.example.airsense.detector.algorithm

import android.util.Log

class FlightDetectionAlgorithm {
    private var lastFlightState = FlightState.GROUNDED
    private var flightStartTime = 0.0
    private var counter = 0
    var flightState = FlightState.GROUNDED
    var firstTimeStamp = 0.0
    private var latestPressures = mutableListOf<Double>()
    private var frequency = 100

    var listener: ((FlightState) -> Unit)? = null

    private fun updateState() {
        if (flightState != lastFlightState) {
            Log.d("FlightDetectionAlgorithm", "[ALGORITHM] Flight state updated to $flightState.")
            listener?.invoke(flightState)
        }
    }

    fun reset() {
        lastFlightState = FlightState.GROUNDED
        flightStartTime = 0.0
        flightState = FlightState.GROUNDED
        firstTimeStamp = 0.0
        latestPressures.clear()
    }

    fun adjustFrequency(newFrequency: Int) {
        println("adjusting frequency")
        frequency = newFrequency
    }

    fun processData(accelerometerData: List<List<Double>>, barometerData: List<List<Double>>) {
        if (accelerometerData.isEmpty() || barometerData.isEmpty()) {
            Log.e("FlightDetectionAlgorithm", "Received empty sensor data for processing")
            return
        }
        if (firstTimeStamp == 0.0) {
            firstTimeStamp = accelerometerData.first()[0]
        }

        detectFlight(accelerometerData, barometerData)
    }

    private fun trimQueueToCarryover(queue: ArrayDeque<List<Double>>, carryOver: Int) {
        while (queue.size > carryOver) queue.removeFirst()
    }

    private fun detectFlight(
        accelerometerData: List<List<Double>>,
        barometerData: List<List<Double>>
    ) {
        val timestamps = accelerometerData.map { it[0] }
        val absoluteAcceleration = accelerometerData.map {
            val (_, x, y, z) = it
            Math.sqrt(x * x + y * y + z * z)
        }

        val smoothedAcceleration =
            movingAverage(absoluteAcceleration, 10 * frequency) //10 seconds moving average
        val varianceAcceleration =
            variance(absoluteAcceleration, 10 * frequency) //10 seconds variance

        // Interpolate barometer data to match accelerometer timestamps
        //val interpolatedBarometerData = interpolateBarometerData(barometerData.toList(), timestamps)
        val interpolatedBarometerData = barometerData.map { it[1] }

        flightState = detectTakeoffState(
            timestamps,
            smoothedAcceleration,
            varianceAcceleration,
            interpolatedBarometerData
        )

        updateState()

        if (flightState != lastFlightState) {
            if (flightState == FlightState.CLIMBING) {
                //we have started flying
                flightStartTime = timestamps.last()
                Log.d("FlightDetectionAlgorithm", "[ALGORITHM] Flight detected. It started at $flightStartTime.")
            } else if (flightState == FlightState.GROUNDED) {
                val flightDuration = timestamps.last() - flightStartTime
                Log.d("FlightDetectionAlgorithm", "[ALGORITHM] Flight ended. It lasted $flightDuration seconds.")
            }
            lastFlightState = flightState
        }
    }

    private fun detectTakeoffState(
        timestamps: List<Double>,
        smoothedAcceleration: List<Double>,
        varianceAcceleration: List<Double>,
        barometerData: List<Double>
    ): FlightState {
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
        var curreentMaxAccel = 0.0

        // Convert nanoseconds to seconds for all timestamps
        val timestampsInSeconds = timestamps.map { it / 1_000_000_000.0 }

        if (flightState == FlightState.CLIMBING) {
            //first lets check if we are climbing
            if (isCruising(barometerData)) {
                Log.d("FlightDetectionAlgorithm", "[ALGORITHM] Cruising detected.")
                return FlightState.CRUISING
            }
            return FlightState.CLIMBING
        } else if (flightState == FlightState.CRUISING) {
            if (isDescending(barometerData)) {
                Log.d("FlightDetectionAlgorithm", "[ALGORITHM] Descending detected.")
                return FlightState.DESCENDING
            }
            return FlightState.CRUISING
        } else if (flightState == FlightState.DESCENDING) {
            //we are approaching the ground
            for (i in smoothedAcceleration.indices) {
                val currentAcceleration = smoothedAcceleration[i]
                if (currentAcceleration > 4.0) {
                    Log.d("FlightDetectionAlgorithm", "Possible landing")
                    println("time of possible landing: ${timestampsInSeconds[i]}")
                    return FlightState.GROUNDED
                }
            }
            return FlightState.DESCENDING
        } else {
            for (i in smoothedAcceleration.indices) {
                var currentAcceleration = smoothedAcceleration[i]
                var currentTime = timestampsInSeconds[i] // Now in seconds
                if (!validTakeoffSpotFound) {
                    if (currentAcceleration in takeoffAccelerationThreshold) {
                        if (!inTakeoff) {
                            inTakeoff = true
                            takeoffStartTime = currentTime
                            currentMaxVariance = 0.0
                            curreentMaxAccel = 0.0
                        }

                        if (varianceAcceleration[i] > currentMaxVariance) {
                            currentMaxVariance = varianceAcceleration[i]
                        }

                        if (currentAcceleration > curreentMaxAccel) {
                            curreentMaxAccel = currentAcceleration
                        }

                        if ((currentTime - takeoffStartTime) >= takeoffDuration) {
                            inTakeoff = false
                            endTime = currentTime
                            if (currentMaxVariance < 1.0) {
                                validTakeoffSpotFound = true
                                Log.d(
                                    "TakeoffDetection",
                                    "Valid takeoff spot found. Time into recording : ${takeoffStartTime - (firstTimeStamp / 1_000_000_000.0)}, Max Variance: $currentMaxVariance, Max Accel: $curreentMaxAccel"
                                )
                                currentMaxVariance = 0.0
                                curreentMaxAccel = 0.0
                            }
                        }
                    } else {
                        inTakeoff = false
                        currentMaxVariance = 0.0
                        curreentMaxAccel = 0.0
                    }
                } else {
//                Log.d("TakeoffDetection", "Checking for acceleration peak after takeoff spot")
                    // Check for acceleration peak within 30 seconds after takeoff spot
                    val peakEndTime = takeoffStartTime + 30.0 // seconds

                    currentTime = timestampsInSeconds[i] // Now in seconds
//                if (currentTime > peakEndTime) {
//                    break
//                }
//                    println("current time - endtime: ${currentTime - endTime}")

                    if (currentTime - endTime <= 30) {
                        currentAcceleration = smoothedAcceleration[i]
                        if (currentAcceleration in 1.5..3.0) {
                            if (peakStartTime == 0.0) {
                                peakStartTime = currentTime
                                Log.d(
                                    "TakeoffDetection",
                                    "Potential acceleration peak start at $currentTime seconds, acceleration: $currentAcceleration"
                                )
                            } else {
//                                Log.d(
//                                    "yoiyo",
//                                    "inside acceleration peak start at $currentTime seconds, acceleration: $currentAcceleration"
//                                )
                                // Check if peak lasts at least 5 seconds

                                if (currentTime - peakStartTime >= 5) {
                                    println("tjena grabben")
                                    // Now, perform the variance check for the next 30 seconds
                                    val varianceCheckEndTime = currentTime + 30.0
                                    finalTime = currentTime

                                    for (j in i until smoothedAcceleration.size) {
                                        //print max variance accel
                                        if (timestampsInSeconds[j] > varianceCheckEndTime) break
                                        if (varianceAcceleration[j] > 3) {
                                            highVarianceFound = true
                                            Log.d(
                                                "TakeoffDetection",
                                                "High variance found at ${timestampsInSeconds[j]} seconds, variance: ${varianceAcceleration[j]}"
                                            )
                                            break
                                        }
                                    }
                                    println("max variance accel: ${varianceAcceleration.max()}")
                                    if (!highVarianceFound) {
                                        Log.d("FlightDetectionAlgorithm", "[ALGORITHM] Takeoff detected.")
                                        Log.d(
                                            "FlightDetectionAlgorithm",
                                            "The takeoff time was ${finalTime - (firstTimeStamp / 1_000_000_000.0)}."
                                        )
                                        latestPressures.clear()
                                        return FlightState.CLIMBING // Takeoff detected
                                    } else {
                                        Log.d(
                                            "TakeoffDetection",
                                            "Takeoff not confirmed due to high variance after acceleration peak."
                                        )
                                    }
                                    validTakeoffSpotFound = false
                                    peakStartTime = 0.0

                                }
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

            return FlightState.GROUNDED
        }
        return FlightState.CLIMBING
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

    fun isCruising(list: List<Double>, tolerance: Double = 2.0): Boolean {
//        println("list: $list")
        if (list.size < 2) return false // Not enough data to determine cruise
        val minPressure = list.minOrNull() ?: return false
        val maxPressure = list.maxOrNull() ?: return false
        // Check if the difference between the min and max pressure is within the tolerance, and convert to positive number always
        val diff = Math.abs(maxPressure - minPressure)
        return diff <= tolerance
    }

    fun isDescending(list: List<Double>): Boolean {
//        println("descending list: $list")
        for (i in 0 until list.size - 1) {
            if (list[i + 1] - list[i] < 0) {
                return false // Not rising by at least 10
            }
        }
        return true // All elements meet the descending condition
    }
}

enum class SensorType {
    ACCELEROMETER, BAROMETER, ORIENTATION
}

enum class FlightState {
    GROUNDED, CRUISING, CLIMBING, DESCENDING
}