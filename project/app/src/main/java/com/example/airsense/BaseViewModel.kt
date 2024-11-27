package com.example.airsense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm

open class BaseViewModel : ViewModel() {
    // Base view model logic goes here
    var absoluteAcceleration by mutableStateOf(0f)
    var accelCurrentTimestamp by mutableStateOf(0L)
    var lastTimestamp by mutableStateOf(0L)
    var accelFirstTimestamp by mutableStateOf(0L)
    var baroFirstTimestamp by mutableStateOf(0L)
    var baroCurrentTimestamp by mutableStateOf(0L)
    var timeBetweenPoints by mutableStateOf(0L)
    var pitch by mutableStateOf(0f)
    var roll by mutableStateOf(0f)
    var yaw by mutableStateOf(0f)
    var pressure by mutableStateOf(0f)

    // Shared flight detection algorithm
    protected val flightDetectionAlgorithm = FlightDetectionAlgorithm()
}