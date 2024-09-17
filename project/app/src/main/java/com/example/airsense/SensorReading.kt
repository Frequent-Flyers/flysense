package com.example.airsense

import android.hardware.Sensor


data class SensorReading(
    private val sensorName: String,
    private val numValues: Int,
    private val time: String,
    private val secondsElapsed: Float,
    private val x: Float? = null,
    private val y: Float? = null,
    private val z: Float? = null,
    private val relativeAltitude: Float? = null,
    private val pressure: Float? = null,
    private val yaw: Float? = null,
    private val qx: Float? = null,
    private val qz: Float? = null,
    private val roll: Float? = null,
    private val qw: Float? = null,
    private val qy: Float? = null,
    private val pitch: Float? = null
) {

    fun getSensorName(): String {
        return sensorName
    }

    fun getSensorType(): Int {
        return when (sensorName) {
            "Accelerometer" -> Sensor.TYPE_ACCELEROMETER
            "Barometer" -> Sensor.TYPE_PRESSURE
            "Orientation" -> Sensor.TYPE_ORIENTATION
            else -> -1
        }
    }

    fun getTime(): Long {
        return time.toLong()
    }

    fun getNumValues(): Int {
        return numValues
    }

    fun getSecondsElapsed(): Float {
        return secondsElapsed
    }

    fun getX(): Float {
        return x!!
    }

    fun getY(): Float {
        return y!!
    }

    fun getZ(): Float {
        return z!!
    }

    fun getRelativeAltitude(): Float {
        return relativeAltitude!!
    }

    fun getPressure(): Float {
        return pressure!!
    }

    fun getYaw(): Float {
        return yaw!!
    }

    fun getQx(): Float {
        return qx!!
    }

    fun getQz(): Float {
        return qz!!
    }

    fun getRoll(): Float {
        return roll!!
    }

    fun getQw(): Float {
        return qw!!
    }

    fun getQy(): Float {
        return qy!!
    }

    fun getPitch(): Float {
        return pitch!!
    }
}