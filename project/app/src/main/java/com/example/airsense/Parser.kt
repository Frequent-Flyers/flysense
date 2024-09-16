package com.example.airsense

import android.content.Context

fun parse(context: Context, sensorName: String, fileName: String): List<SensorData> {
    val assetManager = context.assets
    val input = assetManager.open(fileName)
    val reader = input.bufferedReader()
    val dataFlow = mutableListOf<SensorData>()
    reader.readLine()
    reader.forEachLine { line: String ->
        val data = line.split(",")
        val time = data[0]
        val secondsElapsed = data[1].toFloat()
        val z = data[2].toFloat()
        val y = data[3].toFloat()
        val x = data[4].toFloat()
        val sensorData = SensorData(sensorName, time, secondsElapsed, x, y, z)
        dataFlow.add(sensorData)
    }
    return dataFlow
}