package com.example.airsense.detector.sensors

import android.app.Application
import com.example.airsense.CSVDataLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    // Provide the real accelerometer sensor
    @Provides
    @Singleton
    @Named("realAccelerometerSensor")
    fun provideRealAccelerometerSensor(app: Application): MeasurableSensor {
        return AccelerometerSensor(app)
    }

    // Provide the fake accelerometer sensor
    @Provides
    @Singleton
    @Named("simulatedAccelerometerSensor")
    fun provideSimulatedAccelerometerSensor(@Named("accelerometerCSVLoader") csvDataLoader: CSVDataLoader): MeasurableSensor {
        return SimulatedAccelerometerSensor(csvDataLoader)
    }

    // Provide the orientation sensor
    @Provides
    @Singleton
    @Named("realOrientationSensor")
    fun provideOrientationSensor(app: Application): MeasurableSensor {
        return OrientationSensor(app)
    }

    // Provide the fake orientation sensor
    @Provides
    @Singleton
    @Named("simulatedOrientationSensor")
    fun provideSimulatedOrientationSensor(@Named("orientationCSVLoader") csvDataLoader: CSVDataLoader): MeasurableSensor {
        return SimulatedOrientationSensor(csvDataLoader)
    }

    // Provide the barometer sensor
    @Provides
    @Singleton
    @Named("realBarometerSensor")
    fun provideBarometerSensor(app: Application): MeasurableSensor {
        return BarometerSensor(app)
    }

    // Provide the fake barometer sensor
    @Provides
    @Singleton
    @Named("simulatedBarometerSensor")
    fun provideSimulatedBarometerSensor(@Named("barometerCSVLoader") csvDataLoader: CSVDataLoader): MeasurableSensor {
        return SimulatedBarometerSensor(csvDataLoader)
    }

    // Provide the CSVDataLoader for the fake sensor
    @Provides
    @Singleton
    @Named("accelerometerCSVLoader")
    fun provideCSVDataLoader(application: Application): CSVDataLoader {
        val inputStream = application.assets.open("Accelerometer.csv")
        return CSVDataLoader(inputStream, CSVDataLoader.DataType.ACCELEROMETER)
    }

    // Provide the CSVDataLoader for the orientation sensor
    @Provides
    @Singleton
    @Named("orientationCSVLoader")
    fun provideOrientationCSVDataLoader(application: Application): CSVDataLoader {
        val inputStream = application.assets.open("Orientation.csv")
        return CSVDataLoader(inputStream, CSVDataLoader.DataType.ORIENTATION)
    }

    // Provide the CSVDataLoader for the barometer sensor
    @Provides
    @Singleton
    @Named("barometerCSVLoader")
    fun provideBarometerCSVDataLoader(application: Application): CSVDataLoader {
        val inputStream = application.assets.open("Barometer.csv")
        return CSVDataLoader(inputStream, CSVDataLoader.DataType.BAROMETER)
    }
}