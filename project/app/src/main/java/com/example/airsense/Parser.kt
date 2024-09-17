package com.example.airsense

import android.content.Context

fun parse(context: Context, sensorName: String, fileName: String): List<SensorReading> {
    val assetManager = context.assets
    val input = assetManager.open(fileName)
    val reader = input.bufferedReader()
    val dataFlow = mutableListOf<SensorReading>()
    reader.readLine()
    reader.forEachLine { line: String ->
        val data = line.split(",")
        val time = data[0]
        val secondsElapsed = data[1].toFloat()

        when (data.size) {
            5 -> { // Accelerometer format: time, seconds_elapsed, z, y, x
                val z = data[2].toFloat()
                val y = data[3].toFloat()
                val x = data[4].toFloat()
                val sensorReading = SensorReading(
                    sensorName = sensorName,
                    numValues = 3,
                    time = time,
                    secondsElapsed = secondsElapsed,
                    x = x,
                    y = y,
                    z = z
                )
                dataFlow.add(sensorReading)
            }

            4 -> { // Barometer format: time, seconds_elapsed, relativeAltitude, pressure
                val relativeAltitude = data[2].toFloat()
                val pressure = data[3].toFloat()
                val sensorReading =
                    SensorReading(
                        sensorName = sensorName,
                        numValues = 2,
                        time = time,
                        secondsElapsed = secondsElapsed,
                        relativeAltitude = relativeAltitude,
                        pressure = pressure
                    )
                dataFlow.add(sensorReading)
            }

            9 -> { // Orientation format: time, seconds_elapsed, yaw, qx, qz, roll, qw, qy, pitch
                val yaw = data[2].toFloat()
                val qx = data[3].toFloat()
                val qz = data[4].toFloat()
                val roll = data[5].toFloat()
                val qw = data[6].toFloat()
                val qy = data[7].toFloat()
                val pitch = data[8].toFloat()
                val sensorReading =
                    SensorReading(
                        sensorName = sensorName,
                        numValues = 3,
                        time = time,
                        secondsElapsed = secondsElapsed,
                        yaw = yaw,
                        qx = qx,
                        qz = qz,
                        roll = roll,
                        qw = qw,
                        qy = qy,
                        pitch = pitch
                    )
                dataFlow.add(sensorReading)
            }

            else -> throw IllegalArgumentException("Unknown data format in line: $line")
        }
    }
    return dataFlow
}