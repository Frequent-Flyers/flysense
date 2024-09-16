package com.example.airsense


data class SensorData(
    private val sensorName: String,
    private val time: String,
    private val secondsElapsed: Float,
    private val x: Float,
    private val y: Float,
    private val z: Float,
) {

    fun getSensorName(): String {
        return sensorName
    }

    fun getTime(): Long {
        return time.toLong()
    }

    fun getSecondsElapsed(): Float {
        return secondsElapsed
    }

    fun getX(): Float {
        return x
    }

    fun getY(): Float {
        return y
    }

    fun getZ(): Float {
        return z
    }
}