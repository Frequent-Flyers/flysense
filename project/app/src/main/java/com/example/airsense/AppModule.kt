package com.example.airsense

import android.content.Context
import android.util.Log
import com.example.airsense.data.FlightStateNotifier
import com.example.airsense.data.PreferenceHelper
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun providePreferenceHelper(@ApplicationContext context: Context): PreferenceHelper {
        return PreferenceHelper(context)
    }

    @Singleton
    @Provides
    fun provideFlightDetectionAlgorithm(): FlightDetectionAlgorithm {
        return FlightDetectionAlgorithm()
    }

    @Singleton
    @Provides
    fun provideFlightStateNotifier(
        flightDetectionAlgorithm: FlightDetectionAlgorithm,
        preferenceHelper: PreferenceHelper,
        @ApplicationContext context: Context
    ): FlightStateNotifier {
        return FlightStateNotifier(flightDetectionAlgorithm, preferenceHelper, context)
    }
}