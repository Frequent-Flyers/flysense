package com.example.airsense

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.isAccessible

class Simulator(
    private val sensorManager: SensorManager,
    private val listener: SensorEventListener,
    private val sensors: List<Pair<Int, List<SensorReading>>>,
    private val resampleRate: Int
) {
    private var isRunning = false
    private val sensorEventConstructor =
        SensorEvent::class.constructors.toList()[0].apply { isAccessible = true }

    fun stopSimulation() {
        isRunning = false
    }

    fun runSimulation() {
        isRunning = true
//        val times = sensors[0].map { it.getTime() }
//        val differences = times.zipWithNext().map { it.second - it.first }
//        val sampleRate = 1 / (differences.average() / 1000000000)

        sensors.forEach { (sensorType, sensorData) ->
            sensorData.forEachIndexed { index, item ->
                if (isRunning) {
                    // Use the timestamp difference for timing
                    val delay = if (index > 0) {
                        sensorData[index].getTime() - sensorData[index - 1].getTime()
                    } else 0

                    TimeUnit.NANOSECONDS.sleep(delay) // Simulate time elapsed between readings

                    // Inject the sensor event into the listener
                    val sensorEvent = generateSensorEvent(item, sensorType)
                    listener.onSensorChanged(sensorEvent) // Feed to listener
                }
            }
        }
    }

    private fun generateSensorEvent(data: SensorReading, sensorType: Int): SensorEvent {
        val sensorEvent = sensorEventConstructor.call(3)

        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                sensorEvent.values[0] = data.getX() ?: 0f
                sensorEvent.values[1] = data.getY() ?: 0f
                sensorEvent.values[2] = data.getZ() ?: 0f
            }

            Sensor.TYPE_PRESSURE -> {
                sensorEvent.values[0] = data.getPressure() ?: 0f
            }

            Sensor.TYPE_ORIENTATION -> {
                sensorEvent.values[0] = data.getYaw() ?: 0f
                sensorEvent.values[1] = data.getRoll() ?: 0f
                sensorEvent.values[2] = data.getPitch() ?: 0f
            }
            // Add more sensor types as needed
            else -> throw IllegalArgumentException("Unsupported sensor type: $sensorType")
        }

        // Set the timestamp and sensor
        sensorEvent.timestamp = data.getTime()
//        sensorEvent.sensor = Sensor

        return sensorEvent
    }

    private fun resampleRecording() {

    }
}


