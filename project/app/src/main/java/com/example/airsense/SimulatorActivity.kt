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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airsense.ui.theme.AirsenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SimulatorActivity() : ComponentActivity() {
    private val simulationViewModel: SimulationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirsenseTheme {
                val viewModel = viewModel<SimulationViewModel>()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Greeting("Android")
                        Spacer(modifier = Modifier.height(16.dp))
                        MultiFilePicker(viewModel)
                    }

                    DisplaySensorValues(viewModel)
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
fun MultiFilePicker(viewModel: SimulationViewModel) {
    val context = LocalContext.current
    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val dataStreams = mapOf(
                CSVDataLoader.DataType.ACCELEROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>(),
//                CSVDataLoader.DataType.ORIENTATION to mutableListOf<List<Pair<Long, DoubleArray>>>(),
                CSVDataLoader.DataType.BAROMETER to mutableListOf<List<Pair<Long, DoubleArray>>>()
            )

            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val name = getFileNameFromUri(context.contentResolver, uri)
                    var dataType: CSVDataLoader.DataType = CSVDataLoader.DataType.UNKNOWN

                    Log.d("SimulatorActivity", "File name: $name")

                    if (name != null) {
                        dataType = when {
                            name.contains(
                                "Accelerometer",
                                ignoreCase = true
                            ) -> CSVDataLoader.DataType.ACCELEROMETER

//                            name.contains(
//                                "Orientation",
//                                ignoreCase = true
//                            ) -> CSVDataLoader.DataType.ORIENTATION

                            name.contains(
                                "Barometer",
                                ignoreCase = true
                            ) -> CSVDataLoader.DataType.BAROMETER

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


