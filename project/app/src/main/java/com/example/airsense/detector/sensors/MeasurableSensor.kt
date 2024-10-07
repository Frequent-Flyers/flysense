package com.example.airsense.detector.sensors

abstract class MeasurableSensor(
    protected val sensorType: Int
) {

    protected var onSensorValuesChanged: ((List<Double>) -> Unit)? = null

    abstract val doesSensorExist: Boolean

    abstract fun startListening()
    abstract fun stopListening()

    fun setOnSensorValuesChangedListener(listener: (List<Double>) -> Unit) {
        onSensorValuesChanged = listener
    }

    abstract fun loadData(it: List<Pair<Long, DoubleArray>>)
}