package com.example.airsense.detector.algorithm

class FlightDetectionAlgorithm {

    private var lastAccelerometerData: List<Double>? = null
    private var lastBarometerData: List<Double>? = null
    private var lastOrientationData: List<Double>? = null

    fun onSensorData(sensorType: SensorType, data: List<Double>) {
        when (sensorType) {
            SensorType.ACCELEROMETER -> lastAccelerometerData = data
            SensorType.BAROMETER -> lastBarometerData = data
            SensorType.ORIENTATION -> lastOrientationData = data
        }
        detectFlight()
    }

    private fun detectFlight() {
        if (lastAccelerometerData != null && lastBarometerData != null && lastOrientationData != null) {
            val acceleration = lastAccelerometerData!![0]
            val altitudeChange = lastBarometerData!![0]
            val orientation = lastOrientationData!![0]
        }
    }
}

enum class SensorType {
    ACCELEROMETER, BAROMETER, ORIENTATION
}