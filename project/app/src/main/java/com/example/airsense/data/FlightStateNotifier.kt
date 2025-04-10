package com.example.airsense.data

import android.content.Context
import android.util.Log
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightStateNotifier @Inject constructor(
    private val flightDetectionAlgorithm: FlightDetectionAlgorithm,
    private val preferenceHelper: PreferenceHelper,
    @ApplicationContext private val context: Context
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val stateToText = mapOf(
        "GROUNDED" to "on the ground",
        "CRUISING" to "cruising",
        "CLIMBING" to "climbing",
        "DESCENDING" to "descending"
    )

    init {
        Log.d("FlightStateNotifier", "[NOTIFIER] FDS State Notifier initializing...")

        flightDetectionAlgorithm.addListener { newState ->
            applicationScope.launch {
                val canNotify = preferenceHelper.canNotify.first()

                if (canNotify) {
                    Log.d("FlightStateNotifier", "[NOTIFIER] Sending notification: You are now ${newState.name}.")
                    NotificationHelper.sendNotification(context, "You are now ${stateToText[newState.name]}.")
                }
            }
        }
    }
}