package com.example.airsense

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airsense.ui.theme.AirsenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SimulatorActivity() : ComponentActivity() {
    private lateinit var simulationViewModel: SimulationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirsenseTheme {
                simulationViewModel = viewModel<SimulationViewModel>()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MultiFilePicker(simulationViewModel)
                }

                DisplaySensorValues(simulationViewModel)

            }
        }
    }
}

@Composable
fun MultiFilePicker(viewModel: SimulationViewModel) {
    val context = LocalContext.current
    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val dataStreams = mapOf(
                CSVDataLoader.DataType.ACCELEROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>(),
                CSVDataLoader.DataType.BAROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>()
            )
            var accelerometerData: List<Pair<Long, DoubleArray>>? = null
            var barometerData: List<Pair<Long, DoubleArray>>? = null
            var frequency: Double? = null

            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val name = getFileNameFromUri(context.contentResolver, uri)
                    val dataType = when {
                        name?.contains(
                            "Accelerometer",
                            ignoreCase = true
                        ) == true -> CSVDataLoader.DataType.ACCELEROMETER

                        name?.contains(
                            "Barometer",
                            ignoreCase = true
                        ) == true -> CSVDataLoader.DataType.BAROMETER

                        else -> CSVDataLoader.DataType.UNKNOWN
                    }

                    Log.d("SimulatorActivity", "File name: $name, Data type: $dataType")

                    val csvDataLoader = CSVDataLoader(inputStream, dataType)
                    val result = csvDataLoader.loadData()
                    val data = result.first

                    if (data.isNotEmpty()) {
                        when (dataType) {
                            CSVDataLoader.DataType.ACCELEROMETER -> {
                                accelerometerData = data
                                frequency = result.second
                                viewModel.frequency = frequency!!
                                Log.d(
                                    "SimulatorActivity",
                                    "Loaded accelerometer data: ${data.size} points"
                                )
                            }

                            CSVDataLoader.DataType.BAROMETER -> {
                                barometerData = data
                                Log.d(
                                    "SimulatorActivity",
                                    "Loaded barometer data: ${data.size} points"
                                )
                            }

                            else -> Log.e("SimulatorActivity", "Unknown data type")
                        }
                    }
                }
            }

            // Process data after both accelerometer and barometer are loaded
            if (accelerometerData != null && barometerData != null) {
                Log.d(
                    "SimulatorActivity",
                    "Both files have been loaded -> Interpolating barometerdata"
                )
                val accelerometerTimestamps = accelerometerData!!.map { it.first }
                val interpolatedBarometerData =
                    interpolateBarometerData(barometerData!!, accelerometerTimestamps)
                dataStreams[CSVDataLoader.DataType.ACCELEROMETER]?.add(accelerometerData!!)
                dataStreams[CSVDataLoader.DataType.BAROMETER]?.add(interpolatedBarometerData)
                viewModel.setSimulatedData(dataStreams)
            } else {
                Log.e("SimulatorActivity", "Both files must be selected to proceed")
                if (accelerometerData == null) {
                    Toast.makeText(
                        context,
                        "Please select an accelerometer file.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Please select a barometer file.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    var shouldLaunchPicker by remember { mutableStateOf(false) }

    if (shouldLaunchPicker) {
        documentPicker.launch(arrayOf("*/*")) // all files temporarily
        shouldLaunchPicker = false
    }

    Button(onClick = { shouldLaunchPicker = true }) {
        Text("Pick Multiple Files")
    }
}

fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String? {
    var fileName: String? = null
    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst()) {
            fileName = it.getString(nameIndex)
        }
    }

    return fileName
}


@Composable
fun DisplaySensorValues(viewModel: SimulationViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Absolute acceleration: ${viewModel.absoluteAcceleration}\n" +
                        "Time between points: ${viewModel.timeBetweenPoints}\n" +
                        "Time elapsed ${(viewModel.accelCurrentTimestamp - viewModel.accelFirstTimestamp) / 1000000000}\n",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pressure: ${viewModel.pressure}\n" +
                        "Time elapsed ${(viewModel.baroCurrentTimestamp - viewModel.baroFirstTimestamp) / 1000000000}\n",
            )
        }
    }
}

private fun interpolateBarometerData(
    barometerData: List<Pair<Long, DoubleArray>>, // Original barometer data
    accelerometerTimestamps: List<Long>          // Accelerometer timestamps as reference
): List<Pair<Long, DoubleArray>> {
    if (barometerData.isEmpty() || accelerometerTimestamps.isEmpty()) return emptyList()

    val originalTimestamps = barometerData.map { it.first }
    val originalPressures = barometerData.map { it.second.first() }

    val interpolatedData = accelerometerTimestamps.map { targetTime ->
        val (lowerIndex, upperIndex) = findNearestIndices(barometerData, targetTime)
        val lowerTime = originalTimestamps[lowerIndex]
        val upperTime = originalTimestamps[upperIndex]
        val lowerPressure = originalPressures[lowerIndex]
        val upperPressure = originalPressures[upperIndex]

        val interpolatedPressure = if (lowerTime == upperTime) {
            lowerPressure
        } else {
            val ratio = (targetTime - lowerTime).toDouble() / (upperTime - lowerTime).toDouble()
            lowerPressure + ratio * (upperPressure - lowerPressure)
        }

        Pair(targetTime, doubleArrayOf(interpolatedPressure))
    }

    return interpolatedData
}

private fun findNearestIndices(
    data: List<Pair<Long, DoubleArray>>,
    timestamp: Long
): Pair<Int, Int> {
    val index = data.binarySearch { it.first.compareTo(timestamp) }
    return if (index >= 0) {
        Pair(index, index)
    } else {
        val insertionPoint = -(index + 1)
        Pair(
            maxOf(0, insertionPoint - 1), // Lower bound
            minOf(data.lastIndex, insertionPoint) // Upper bound
        )
    }
}


