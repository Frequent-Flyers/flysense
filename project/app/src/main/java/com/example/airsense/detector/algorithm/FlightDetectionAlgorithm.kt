package com.example.airsense.detector.algorithm

class FlightDetectionAlgorithm {

    // Hold the last N data points for each sensor
    private val maxDataPoints = 100
    private val accelerometerDataQueue: ArrayDeque<List<Double>> = ArrayDeque(maxDataPoints)
    private val barometerDataQueue: ArrayDeque<List<Double>> = ArrayDeque(maxDataPoints)
    private val orientationDataQueue: ArrayDeque<List<Double>> = ArrayDeque(maxDataPoints)

    fun reset() {
        accelerometerDataQueue.clear()
        barometerDataQueue.clear()
        orientationDataQueue.clear()
    }

    fun onSensorData(sensorType: SensorType, data: List<Double>) {
        when (sensorType) {
            SensorType.ACCELEROMETER -> addDataToQueue(accelerometerDataQueue, data)
            SensorType.BAROMETER -> addDataToQueue(barometerDataQueue, data)
            SensorType.ORIENTATION -> addDataToQueue(orientationDataQueue, data)
        }
        detectFlight()
    }

    // Helper function to add data to the queue and maintain the fixed size
    private fun addDataToQueue(queue: ArrayDeque<List<Double>>, data: List<Double>) {
        if (queue.size >= maxDataPoints) {
            queue.removeFirst()  // Remove the oldest data point
        }
        queue.addLast(data)  // Add the newest data point
    }

    private fun detectFlight() {
        // Only proceed if we have enough data points in all queues
        if (accelerometerDataQueue.size == maxDataPoints &&
            barometerDataQueue.size == maxDataPoints &&
            orientationDataQueue.size == maxDataPoints
        ) {

            // Analyze the data change over time
            val accelerationChanges = analyzeDataChange(accelerometerDataQueue)
            val altitudeChanges = analyzeDataChange(barometerDataQueue)
            val orientationChanges = analyzeDataChange(orientationDataQueue)
            println("Acceleration changes: $accelerationChanges")

            // Implement flight detection logic here based on the changes
//            if (accelerationChanges > threshold && altitudeChanges > threshold && orientationChanges < threshold) {
//                println("Flight detected!")
//            }

        }
    }

    // Example method to analyze changes in data over time
    private fun analyzeDataChange(queue: ArrayDeque<List<Double>>): Double {
        // Example: Calculate the difference between the first and last data point
        val first = queue.first()[0]
        val last = queue.last()[0]
        return last - first  // Simple change over time, can be more complex
    }
}

enum class SensorType {
    ACCELEROMETER, BAROMETER, ORIENTATION
}