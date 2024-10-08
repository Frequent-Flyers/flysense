package com.example.airsense

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.airsense.ui.theme.AirsenseTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class SimulatorActivity() : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirsenseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Greeting("Android")
                        Spacer(modifier = Modifier.height(16.dp))
                        MultiFilePicker(mainViewModel)
                    }

                    DisplaySensorValues(mainViewModel)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AirsenseTheme {
        Greeting("Android")
    }
}

@Composable
fun MultiFilePicker(viewModel: MainViewModel) {
    val context = LocalContext.current
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val dataStreams = mapOf(
            CSVDataLoader.DataType.ACCELEROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>(),
            CSVDataLoader.DataType.ORIENTATION to mutableListOf<List<Pair<Long, DoubleArray>>>(),
            CSVDataLoader.DataType.BAROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>()
        )

        uris.forEach { uri ->
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val name = getFileNameFromUri(context.contentResolver, uri)
                var dataType: CSVDataLoader.DataType = CSVDataLoader.DataType.UNKNOWN

                Log.d("SimulatorActivity", "File name: $name")

                if (name != null) {
                    dataType = when {
                        name.contains("Accelerometer", ignoreCase = true) -> CSVDataLoader.DataType.ACCELEROMETER
                        name.contains("Orientation", ignoreCase = true) -> CSVDataLoader.DataType.ORIENTATION
                        name.contains("Barometer", ignoreCase = true) -> CSVDataLoader.DataType.BAROMETER
                        else -> CSVDataLoader.DataType.UNKNOWN
                    }
                }

                Log.d("SimulatorActivity", "Data type: $dataType")

                val csvDataLoader = CSVDataLoader(inputStream, dataType)
                val data = csvDataLoader.loadData()

                if (data.isNotEmpty()) {
                    dataStreams[dataType]?.add(data)
                }
            }
        }

        viewModel.setSimulatedData(dataStreams)
    }

    // track if the picker should be launched
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
fun DisplaySensorValues(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (viewModel.absoluteAcceleration != 0f || viewModel.pitch != 0f || viewModel.roll != 0f || viewModel.yaw != 0f || viewModel.pressure != 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Absolute acceleration: ${viewModel.absoluteAcceleration}\n" +
                            "Current timestamp: ${viewModel.currentTimestamp}\n" +
                            "Time between points: ${viewModel.timeBetweenPoints}",
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
                    text = "Pitch: ${viewModel.pitch}\n" +
                            "Roll: ${viewModel.roll}\n" +
                            "Yaw: ${viewModel.yaw}",
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
                    text = "Pressure: ${viewModel.pressure}\n",
                )
            }
        } else {
            Text("No sensor data loaded. Please load CSV files.")
        }
    }
}


