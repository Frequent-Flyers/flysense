package com.example.airsense

import android.content.ContentResolver
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import com.example.airsense.detector.algorithm.FlightState
import com.example.airsense.ui.theme.AirsenseTheme
import dagger.hilt.android.AndroidEntryPoint
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow

var finalTimestamp = 0L
var stageDetectionTimes = mutableMapOf<String, String>()

data class BottomNavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@AndroidEntryPoint
class SimulatorActivity : ComponentActivity() {
    private lateinit var simulationViewModel: SimulationViewModel
    private lateinit var flightDetectionAlgorithm: FlightDetectionAlgorithm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setImmersiveMode()

        setContent {
            AirsenseTheme {
                simulationViewModel = viewModel<SimulationViewModel>()
                flightDetectionAlgorithm = simulationViewModel.flightDetectionAlgorithm
                var selectedItemIndex by remember { mutableIntStateOf(1) }
                var selectedSensor by remember { mutableStateOf(setOf<String>()) }
                var flightState by remember { mutableStateOf(FlightState.GROUNDED) }

                flightDetectionAlgorithm.listener = { newFlightState ->
                    Log.d("SimulatorActivity", "[SIMULATOR] New flight state: $newFlightState")
                    flightState = newFlightState
                }

                val items = listOf(
                    BottomNavigationItem(
                        title = "Fly",
                        selectedIcon = ImageVector.vectorResource(id = R.drawable.fly),
                        unselectedIcon = ImageVector.vectorResource(id = R.drawable.fly),
                    ),
                    BottomNavigationItem(
                        title = "Simulate",
                        selectedIcon = ImageVector.vectorResource(id = R.drawable.simulate),
                        unselectedIcon = ImageVector.vectorResource(id = R.drawable.simulate),
                    ),
                    BottomNavigationItem(
                        title = "Settings",
                        selectedIcon = ImageVector.vectorResource(id = R.drawable.settings),
                        unselectedIcon = ImageVector.vectorResource(id = R.drawable.settings),
                    )
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedItemIndex == index,
                                    onClick = {
                                        if (index == 0) {
                                            finish()
                                        } else if (index == 1) {
                                            selectedItemIndex = index
                                        } else if (index == 2) {
                                            Log.d("SimulatorActivity", "Settings clicked")
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedItemIndex == index) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    label = {
                                        Text(text = item.title)
                                    }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = paddingValues.calculateBottomPadding())
                                .verticalScroll(rememberScrollState())
                        ) {

                            val elapsedTime = (simulationViewModel.accelCurrentTimestamp - simulationViewModel.accelFirstTimestamp) / 1000000000

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
                                    stageDetectionTimes = stageDetectionTimes,
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
                                        onSensorSelectionChange = { selectedSensor = it },
                                        selectedSensors = selectedSensor
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
fun SimulationProgressCard(
    viewModel: SimulationViewModel
) {
    val elapsed = viewModel.accelCurrentTimestamp - viewModel.accelFirstTimestamp
    val hours = elapsed / 3600000000000
    val minutes = (elapsed % 3600000000000) / 60000000000
    val seconds = (elapsed % 60000000000) / 1000000000

    val totalDuration = finalTimestamp - viewModel.accelFirstTimestamp

    val formattedTime = when {
        hours > 0 -> String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        else -> String.format(Locale.getDefault(), "00:%02d", seconds)
    }

    val progress = (elapsed.toDouble() / totalDuration.toDouble() * 100).toInt()
    val progressColor = Color(0xFF9a78d1)


    val seaLevelPressure = 1013.25 // hPa
    val estimatedCabinAltitudeMeters = 44330 * (1 - (viewModel.pressure / seaLevelPressure).pow(1 / 5.255))
    val estimatedCabinAltitudeFeet = estimatedCabinAltitudeMeters * 3.28084

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress / 100f)
                    .background(progressColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Simulation Progress",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "${progress}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Column {
                Text(
                    text = "Accelerometer",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Acceleration: ${viewModel.absoluteAcceleration.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()} m/s²",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Average Variance: ${viewModel.timeBetweenPoints.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()} m/s³",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Barometer",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pressure: ${viewModel.pressure.toInt()} hPa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Cabin Altitude: ${estimatedCabinAltitudeMeters.toInt()} m (${estimatedCabinAltitudeFeet.toInt()} ft)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    stageDetectionTimes: MutableMap<String, String>,
    elapsedSimulationTime: String,
    onEndSimulationClick: () -> Unit
) {
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

    var hasTakenOff by remember { mutableStateOf(false) }

    if (currentStage == "CLIMBING" || currentStage == "CRUISING" || currentStage == "LANDING") {
        hasTakenOff = true
    }

    if (currentStage !in stageDetectionTimes && (currentStage != "GROUNDED" || hasTakenOff)) {
        stageDetectionTimes[currentStage] = elapsedSimulationTime
    }

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
                            if (stage in stageDetectionTimes) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f))
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
                                if (stage in stageDetectionTimes) MaterialTheme.colorScheme.primary else Color.Gray
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
                            color = if (stage in stageDetectionTimes) MaterialTheme.colorScheme.primary else Color.Gray
                        )

                        Text(
                            text = if (stage in stageDetectionTimes) {
                                "${stageDetectionTimes[stage]} elapsed"
                            } else {
                                "Not Detected"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (stage in stageDetectionTimes) MaterialTheme.colorScheme.primary else Color.Gray
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
                viewModel.clearFromPreviousRuns()
                finalTimestamp = accelerometerTimestamps.last()
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


