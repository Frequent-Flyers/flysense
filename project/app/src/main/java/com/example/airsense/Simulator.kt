package com.example.airsense

import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.concurrent.TimeUnit

//import kotlin.reflect.jvm.isAccessible


class Simulator(
    private val sensorManager: SensorManager,
    private val listener: SensorEventListener, // The real sensor event listener
    private val sensorType: Int,
    private val sensorData: List<SensorData>,
    private val resampleRate: Int
) {
    private var isRunning = false
//    private val sensorEventConstructor =
//        SensorEvent::class.constructors.toList()[0].apply { isAccessible = true }


//    private fun generateSensorEvent(dataItem: SensorData, sensor: Sensor) {
//        val event = SensorEvent(sensor, dataItem.getX(), dataItem.getY(), dataItem.getZ())
//
//    }

    fun stopSimulation() {
        isRunning = false
    }

    fun runSimulation() {
        isRunning = true
//        val times = sensors[0].map { it.getTime() }
//        val differences = times.zipWithNext().map { it.second - it.first }
//        val sampleRate = 1 / (differences.average() / 1000000000)

        sensorData.forEachIndexed { index, item ->
            if (isRunning) {
                // Use the timestamp difference for timing
                val delay = if (index > 0) {
                    sensorData[index].getTime() - sensorData[index - 1].getTime()
                } else 0

                TimeUnit.NANOSECONDS.sleep(delay) // Simulate time elapsed between readings

                // Inject the sensor event into the listener
                val sensorEvent = generateSensorEvent(item)
                listener.onSensorChanged(sensorEvent) // Feed to listener
            }
        }
    }

    private fun generateSensorEvent(data: SensorData): SensorEvent {
        val sensorEvent =
            SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
                .newInstance(3)
        // Modify the existing values array directly
        sensorEvent.values[0] = data.getX()
        sensorEvent.values[1] = data.getY()
        sensorEvent.values[2] = data.getZ()

        // Set the timestamp and sensor
        sensorEvent.timestamp = data.getTime()
//        sensorEvent.sensor = sensorType

        return sensorEvent
    }

    private fun resampleRecording() {

    }
}

