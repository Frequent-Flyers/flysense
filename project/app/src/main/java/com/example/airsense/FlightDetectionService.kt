package com.example.airsense

import android.annotation.SuppressLint
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import com.example.airsense.data.NotificationHelper
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import com.example.airsense.detector.sensors.MeasurableSensor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class FlightDetectionService : LifecycleService() {

    @Inject lateinit var flightDetectionAlgorithm: FlightDetectionAlgorithm
    @Inject @Named("realAccelerometerSensor") lateinit var accelerometer: MeasurableSensor
    @Inject @Named("realBarometerSensor") lateinit var barometer: MeasurableSensor

    private var stateJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        startForeground(NOTIFICATION_ID, buildOngoingNotification("grounded"))

        // Hook up listeners, reuse logic from MainViewModel directly here
        accelerometer.startListening()
        barometer.startListening()

        flightDetectionAlgorithm.addListener {
            updateNotification(it.name.lowercase())
        }
    }

    override fun onDestroy() {
        accelerometer.stopListening()
        barometer.stopListening()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(state: String) {
        val updated = buildOngoingNotification(state)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, updated)
    }

    private fun buildOngoingNotification(state: String): Notification {
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("FDS")
            .setContentText("You are currently $state.")
            .setSmallIcon(R.drawable.flysense_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }
}
