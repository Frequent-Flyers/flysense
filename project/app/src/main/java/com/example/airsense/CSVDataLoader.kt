package com.example.airsense

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class CSVDataLoader(private val inputStream: InputStream, private val dataType: DataType) {

    enum class DataType {
        ACCELEROMETER, ORIENTATION, BAROMETER, UNKNOWN
    }

    fun loadData(): List<Pair<Long, DoubleArray>> {
        val result = mutableListOf<Pair<Long, DoubleArray>>()
        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.useLines { lines ->
            lines.drop(1).forEach { line -> // Drop the header row
                val values = line.split(",")

                when (dataType) {
                    DataType.ACCELEROMETER -> {
                        // Timestamp is in the first column
                        val timestamp = values[0].toLong()
                        // x, y, z values are in the 5th, 4th, and 3rd columns, respectively
                        val x = values[4].toDouble()
                        val y = values[3].toDouble()
                        val z = values[2].toDouble()
                        //create list and add timestamp and x, y, z data
                        var list = listOf(timestamp, x, y, z)
                        result.add(
                            Pair(
                                timestamp,
                                doubleArrayOf(x, y, z)
                            )
                        ) // Add timestamp and x, y, z data
                    }

                    DataType.ORIENTATION -> {
                        // Timestamp is in the first column
                        val timestamp = values[0].toLong()
                        // yaw, qx, qz, roll, qw, qy, pitch values are in the 3rd to 8th columns
                        val yaw = values[2].toDouble()
                        val roll = values[5].toDouble()
                        val pitch = values[8].toDouble()
                        //create list and add timestamp and yaw, qx, qz, roll, qw, qy, pitch data
                        var list = listOf(timestamp, yaw, roll, pitch)
                        result.add(
                            Pair(
                                timestamp,
                                doubleArrayOf(yaw, roll, pitch)
                            )
                        ) // Add timestamp and yaw, qx, qz, roll, qw, qy, pitch data
                    }

                    DataType.BAROMETER -> {
                        // Timestamp is in the first column
                        val timestamp = values[0].toLong()
                        // Pressure value is in the 4th column
                        val pressure = values[3].toDouble()
                        //create list and add timestamp and pressure data
                        var list = listOf(timestamp, pressure)
                        result.add(
                            Pair(
                                timestamp,
                                doubleArrayOf(pressure)
                            )
                        )
                    }

                    DataType.UNKNOWN -> {
                        Log.e("CSVDataLoader", "Unknown data type.")
                    }
                }
            }


            Log.d("CSVDataLoader", "Loaded ${result.size} data points for $dataType.")

            return result
        }
    }
}