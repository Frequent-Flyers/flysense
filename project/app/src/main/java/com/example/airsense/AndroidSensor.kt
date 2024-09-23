package com.example.airsense

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

abstract class AndroidSensor(
    private val context: Context,
    private val sensorFeature: String,
    sensorType: Int
): MeasurableSensor(sensorType), SensorEventListener {

    override val doesSensorExist: Boolean
        get() = context.packageManager.hasSystemFeature(sensorFeature)

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    override fun startListening() {
        if(!doesSensorExist) {
            return
        }
        if(!::sensorManager.isInitialized && sensor == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensor = sensorManager.getDefaultSensor(sensorType)
        }
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun stopListening() {
        if(!doesSensorExist || !::sensorManager.isInitialized) {
            return
        }
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!doesSensorExist) {
            return
        }
        if(event?.sensor?.type == sensorType) {
//            // Convert SensorEvent.timestamp to Unix time (milliseconds)
//            val bootTimeMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()
//            val unixTime = bootTimeMillis + event.timestamp / 1_000_000L
//
            // Dynamically build a list that includes the timestamp and the sensor values
            val sensorData = mutableListOf<Double>()
//            sensorData.add(unixTime.toFloat()) // Add the timestamp
            sensorData.add(event.timestamp.toDouble())

            // Add all the sensor values dynamically
            sensorData.addAll(event.values.map { it.toDouble() })

            // Pass the timestamp and sensor values to the listener
            onSensorValuesChanged?.invoke(sensorData)
//              onSensorValuesChanged?.invoke(event.values.toList())
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) = Unit
}