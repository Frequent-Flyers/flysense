package com.example.airsense

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.reflect.jvm.isAccessible

class CustomSensorManager(
    private val sensorManager: SensorManager,
    private val data: List<List<SensorReading>>,
    private val resample: Boolean = false,
) : SensorManagerInterface, Runnable {

    private val sensorMap = HashMap<Int, Sensor>() // Map sensor type with Sensor object
    private val samplingPeriodNsMap = HashMap<Int, Long>() // Map for holding sampling periods
    private val listeners = HashMap<SensorEventListener, Int>() // Holds the listeners
    private val listenerByTypeMap = HashMap<Int, SensorEventListener>() //SensorType:Listener
    private val eventCount = HashMap<Int, Int>() //SensorType:NumberOfEvents
    private var running = false
    private var currentIndex = 0
    private var stop = false

    private val sensorEventConstructor =
        SensorEvent::class.constructors.toList()[0].apply { isAccessible = true }

    override fun getDefaultSensor(type: Int): Sensor {
        return sensorManager.getDefaultSensor(type)!!
    }

    override fun registerListener(
        listener: SensorEventListener,
        sensor: Sensor,
        samplingPeriodUs: Int,
        maxReportLatencyUs: Int
    ): Boolean {
        sensorMap[sensor.type] = sensor
        if (eventCount[sensor.type] == null) eventCount[sensor.type] = 0
        samplingPeriodNsMap[sensor.type] = samplingPeriodUs.toLong() * 1000

        listeners[listener] = sensor.type
        listenerByTypeMap[sensor.type] = listener
        if (!running) {
            Thread(this).start()
            running = true
        }
        return true
    }

    override fun unregisterListener(listener: SensorEventListener) {
        val type = listeners[listener]
        listeners.remove(listener)
        listenerByTypeMap.remove(type)
        sensorMap.remove(type)
        if (listeners.isEmpty()) {
            stop = true
        }
    }

    private fun generateSensorEvent(reading: SensorReading): SensorEvent {
        return sensorEventConstructor.call(reading.getNumValues()).apply {
            sensor = sensorManager.getDefaultSensor(reading.getSensorType())
            timestamp = reading.getTime() / 1000
            when (reading.getSensorType()) {
                Sensor.TYPE_ACCELEROMETER -> {
                    values[0] = reading.getX()
                    values[1] = reading.getY()
                    values[2] = reading.getZ()
                }

                Sensor.TYPE_PRESSURE -> {
                    values[0] = reading.getPressure()
                    values[1] = reading.getRelativeAltitude()
                }

                Sensor.TYPE_ORIENTATION -> {
                    values[0] = reading.getYaw()
                    values[1] = reading.getPitch()
                    values[2] = reading.getRoll()
                }
            }
        }
    }

    override fun run() {
        Thread.sleep(200)

        if (resample) {
//            resample(lines)
        } else {
            data.forEach { sensorData ->
                sensorData.forEach { item ->
                    // Use the timestamp difference for timing
//                    val delay = if (index > 0) {
//                        sensorData[index].getTime() - sensorData[index - 1].getTime()
//                    } else 0
//
//                    TimeUnit.NANOSECONDS.sleep(delay) // Simulate time elapsed between readings

                    // Inject the sensor event into the listener
                    val sensorEvent = generateSensorEvent(item)
                    sendSensorEvent(sensorEvent) // Feed to listener
                }
            }
        }
        Log.i("dog", "Replay complete")
        running = false
        stop = false
    }

    private fun sendSensorEvent(event: SensorEvent) {
        if (event.sensor == null) return
        val listener = listenerByTypeMap[event.sensor.type]
        if (listener != null) {
            listenerByTypeMap[event.sensor.type]?.onSensorChanged(event)
            val count = eventCount[event.sensor.type]
            if (count != null) eventCount[event.sensor.type] = count + 1
        }
    }
}