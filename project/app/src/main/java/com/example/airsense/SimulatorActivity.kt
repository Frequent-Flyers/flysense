package com.example.airsense

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airsense.FlightStageManager.hasTakenOff
import com.example.airsense.FlightStageManager.stageDetectionTimes
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import com.example.airsense.detector.algorithm.FlightState
import com.example.compose.AirSenseTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

var finalTimestamp = 0L
val selectedSensors = mutableStateOf(setOf<String>())
val selectedFiles = mutableSetOf<String>()

data class BottomNavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Manages the flight stages and their detection times
 * @property stageDetectionTimes A map of flight stages and their detection times
 * @property hasTakenOff A flag indicating whether the aircraft has taken off
 */
object FlightStageManager {
    private val _stageDetectionTimes = mutableStateOf<Map<String, String>>(emptyMap())
    val stageDetectionTimes: Map<String, String> get() = _stageDetectionTimes.value

    var hasTakenOff = false
        private set

    /**
     * Update the stage detection times
     * @param currentStage The current stage
     * @param elapsedSimulationTime The elapsed simulation time
     */
    fun updateStage(currentStage: String, elapsedSimulationTime: String) {
        if (currentStage == "CLIMBING" || currentStage == "CRUISING" || currentStage == "DESCENDING") {
            hasTakenOff = true
        }

        if (currentStage == "GROUNDED" && !hasTakenOff) {
            return
        }

        if (currentStage !in stageDetectionTimes) {
            _stageDetectionTimes.value =
                stageDetectionTimes + (currentStage to elapsedSimulationTime)
        }

        if (currentStage == "GROUNDED" && hasTakenOff) {
            _stageDetectionTimes.value = stageDetectionTimes + ("GROUNDED" to elapsedSimulationTime)
        }
    }
}

@AndroidEntryPoint
class SimulatorActivity : ComponentActivity() {
    private lateinit var simulationViewModel: SimulationViewModel
    private lateinit var flightDetectionAlgorithm: FlightDetectionAlgorithm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setImmersiveMode()

        setContent {
            AirSenseTheme {
                simulationViewModel = viewModel<SimulationViewModel>()
                flightDetectionAlgorithm = simulationViewModel.flightDetectionAlgorithm
                var selectedItemIndex by remember { mutableIntStateOf(1) }
                var selectedSensor by remember { mutableStateOf(setOf<String>()) }
                var flightState by remember { mutableStateOf(FlightState.GROUNDED) }

                flightDetectionAlgorithm.listener = { newFlightState ->
                    Log.d("SimulatorActivity", "[SIMULATOR] New flight state: $newFlightState")
                    flightState = newFlightState
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(
                            selectedItemIndex = selectedItemIndex,
                            onItemSelected = { index ->
                                if (index == 0) {
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                }
                                if (index == 1) finish()
                                if (index == 2) Log.d("SimulatorActivity", "Settings clicked")
                            }
                        )
                    }

                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = paddingValues.calculateBottomPadding())
                                .verticalScroll(rememberScrollState())
                        ) {

                            val elapsedTime =
                                (simulationViewModel.accelCurrentTimestamp - simulationViewModel.accelFirstTimestamp) / 1000000000

                            if (simulationViewModel.accelFirstTimestamp != 0L) {
                                SimulationProgressCard(viewModel = simulationViewModel)
                                FlightStageCard(
                                    currentStage = flightState.toString(),
                                    elapsedSimulationTime = String.format(
                                        Locale.getDefault(),
                                        "%02d:%02d:%02d",
                                        elapsedTime / 3600,
                                        (elapsedTime % 3600) / 60,
                                        elapsedTime % 60
                                    ),
                                    onEndSimulationClick = {

                                    }
                                )
                            }
                        }

                        if (simulationViewModel.accelFirstTimestamp == 0L) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SimulationSensorsCard(
                                        onSensorSelectionChange = { selectedSensors.value = it },
                                        selectedSensors = selectedSensors.value
                                    )

                                    MultiFilePicker(simulationViewModel)
                                }
                            }

                            Log.d("Selected Sensors", selectedSensor.toString())
                        }
                    }
                }
            }
        }
    }


    private fun setImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false) // Let your app handle window insets
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars()) // Hides both status and navigation bars
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION") // compatibility for older Android versions
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }
}

@Composable
fun SimulationProgressCard(viewModel: SimulationViewModel) {
    val elapsed = viewModel.accelCurrentTimestamp - viewModel.accelFirstTimestamp
    val formattedTime = Duration.ofNanos(elapsed).toFormattedString()
    val progress = calculateProgress(elapsed, finalTimestamp - viewModel.accelFirstTimestamp)

    val stageDetectionTimes = FlightStageManager.stageDetectionTimes
    val hasEnded = FlightStageManager.hasTakenOff && "GROUNDED" in stageDetectionTimes

    val containerColor = if (hasEnded) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (hasEnded) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = textColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasEnded) "Flight Ended" else "Simulation Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                if (hasEnded) {
                    Text(
                        text = "Duration: ${
                            calculateDuration(
                                stageDetectionTimes["CLIMBING"] ?: "00:00:00",
                                stageDetectionTimes["GROUNDED"] ?: "00:00:00"
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Indicator or Static Box for Ended State
            if (!hasEnded) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Composable
fun SimulationSensorsCard(
    onSensorSelectionChange: (Set<String>) -> Unit,
    selectedSensors: Set<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Simulation Sensors",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Sensor buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Accelerometer Button
                SensorButton(
                    label = "Accelerometer",
                    icon = ImageVector.vectorResource(id = R.drawable.accelerometer),
                    isSelected = selectedSensors.contains("Accelerometer"),
                    onClick = {
                        val updatedSelection = if (selectedSensors.contains("Accelerometer")) {
                            selectedSensors - "Accelerometer"
                        } else {
                            selectedSensors + "Accelerometer"
                        }
                        onSensorSelectionChange(updatedSelection)
                    }
                )

                // Barometer Button
                SensorButton(
                    label = "Barometer",
                    icon = ImageVector.vectorResource(id = R.drawable.barometer),
                    isSelected = selectedSensors.contains("Barometer"),
                    onClick = {
                        val updatedSelection = if (selectedSensors.contains("Barometer")) {
                            selectedSensors - "Barometer"
                        } else {
                            selectedSensors + "Barometer"
                        }
                        onSensorSelectionChange(updatedSelection)
                    }
                )
            }
        }
    }
}

@Composable
fun FlightStageCard(
    currentStage: String,
    elapsedSimulationTime: String,
    onEndSimulationClick: () -> Unit
) {
    // Update the flight stage globally
    FlightStageManager.updateStage(currentStage, elapsedSimulationTime)

    val stageIcons = listOf(
        R.drawable.takeoff to "CLIMBING", // Takeoff
        R.drawable.cruising to "CRUISING", // Cruise
        R.drawable.landing to "DESCENDING", // Descent
        R.drawable.grounded to "GROUNDED" // Landing
    )

    val stageDisplayNames = mapOf(
        "CLIMBING" to "Takeoff",
        "CRUISING" to "Cruise",
        "DESCENDING" to "Descent",
        "GROUNDED" to "Landing"
    )

    val stageDetectionTimes = FlightStageManager.stageDetectionTimes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            stageIcons.forEach { (iconRes, stage) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (stage in stageDetectionTimes) Color.White.copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.15f)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = iconRes),
                        contentDescription = stage,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (stage in stageDetectionTimes) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                            )
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stageDisplayNames[stage] ?: stage,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (stage in stageDetectionTimes) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = if (stage in stageDetectionTimes) {
                                "${stageDetectionTimes[stage]} elapsed"
                            } else {
                                "Not Detected"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (stage in stageDetectionTimes) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEndSimulationClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End Simulation")
            }
        }
    }
}


@Composable
fun SensorButton(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 4.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun MultiFilePicker(viewModel: SimulationViewModel) {
    val context = LocalContext.current
    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            // reset states each time the user selects new files
            selectedFiles.clear()
            viewModel.clearFromPreviousRuns()

            val requiredFiles = mutableSetOf<String>()
            val dataStreams = mapOf(
                CSVDataLoader.DataType.ACCELEROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>(),
                CSVDataLoader.DataType.BAROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>()
            )

            // determine required files
            if ("Accelerometer" in selectedSensors.value) {
                requiredFiles.add("Accelerometer")
            }
            if ("Barometer" in selectedSensors.value) {
                requiredFiles.add("Barometer")
            }

            var accelerometerData: List<Pair<Long, DoubleArray>>? = null
            var barometerData: List<Pair<Long, DoubleArray>>? = null
            var frequency: Double? = null
            var fileErrorMessage: String? = null

            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileName = getFileNameFromUri(context.contentResolver, uri)
                    val dataType = when {
                        fileName?.contains("Accelerometer", ignoreCase = true) == true -> {
                            selectedFiles.add("Accelerometer")
                            CSVDataLoader.DataType.ACCELEROMETER
                        }

                        fileName?.contains("Barometer", ignoreCase = true) == true -> {
                            selectedFiles.add("Barometer")
                            CSVDataLoader.DataType.BAROMETER
                        }

                        else -> CSVDataLoader.DataType.UNKNOWN
                    }

                    if (dataType == CSVDataLoader.DataType.UNKNOWN) {
                        fileErrorMessage = "The file '$fileName' is unrecognized."
                        return@forEach
                    }

                    val csvDataLoader = CSVDataLoader(inputStream, dataType)
                    val (data, detectedFrequency) = csvDataLoader.loadData()

                    if (data.isEmpty()) {
                        fileErrorMessage = "The file '$fileName' contains no valid data."
                        return@forEach
                    }

                    when (dataType) {
                        CSVDataLoader.DataType.ACCELEROMETER -> {
                            accelerometerData = data
                            frequency = detectedFrequency
                        }

                        CSVDataLoader.DataType.BAROMETER -> {
                            barometerData = data
                        }

                        else -> Unit
                    }
                }
            }

            // check for missing files relative to selected sensors
            val missingFiles = requiredFiles - selectedFiles
            if (missingFiles.isNotEmpty()) {
                Toast.makeText(
                    context,
                    "Missing file(s): ${missingFiles.joinToString()}",
                    Toast.LENGTH_SHORT
                ).show()
                return@rememberLauncherForActivityResult
            }

            if (requiredFiles.size == 1) {
                val selectedSensor = requiredFiles.first()
                if (selectedSensor == "Accelerometer" && accelerometerData == null) {
                    Toast.makeText(context, "Please select a valid Accelerometer file.", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
                if (selectedSensor == "Barometer" && barometerData == null) {
                    Toast.makeText(context, "Please select a valid Barometer file.", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
            }

            if (accelerometerData != null && barometerData != null) {
                val safeAccelerometerData = accelerometerData
                val safeBarometerData = barometerData
                val accelerometerTimestamps = safeAccelerometerData?.map { it.first }

                val interpolatedBarometerData = interpolateBarometerData(safeBarometerData!!, accelerometerTimestamps!!)
                dataStreams[CSVDataLoader.DataType.ACCELEROMETER]?.add(safeAccelerometerData)
                dataStreams[CSVDataLoader.DataType.BAROMETER]?.add(interpolatedBarometerData)

                viewModel.setSimulatedData(dataStreams)
                finalTimestamp = accelerometerTimestamps.last()
            } else {
                Toast.makeText(context, "Please select the required files.", Toast.LENGTH_SHORT).show()
            }
        }

    var shouldLaunchPicker by remember { mutableStateOf(false) }

    if (shouldLaunchPicker) {
        documentPicker.launch(arrayOf("*/*"))
        shouldLaunchPicker = false
    }

    val areSensorsSelected = selectedSensors.value.isNotEmpty()

    Button(
        onClick = { shouldLaunchPicker = true },
        enabled = areSensorsSelected,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (areSensorsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (areSensorsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
    ) {
        Text("Select Flight Files")
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
                text = "Recorded Pressure: ${viewModel.pressure}\n" +
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

fun calculateDuration(startTime: String, endTime: String): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    val start = LocalTime.parse(startTime, formatter)
    val end = LocalTime.parse(endTime, formatter)

    val duration = Duration.between(start, end)

    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    // Format and return the result
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

fun calculateProgress(elapsed: Long, totalDuration: Long): Float {
    if (totalDuration <= 0) return 0f
    return (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
}

fun Duration.toFormattedString(): String {
    val hours = this.toHours()
    val minutes = this.toMinutes() % 60
    val seconds = this.seconds % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
