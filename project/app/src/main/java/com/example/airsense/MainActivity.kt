package com.example.airsense

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airsense.data.FlightStateNotifier
import com.example.airsense.data.NotificationHelper
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import com.example.airsense.detector.algorithm.FlightState
import com.example.compose.AirSenseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.Manifest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var flightDetectionAlgorithm: FlightDetectionAlgorithm
    @Inject lateinit var flightStateNotifier: FlightStateNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // notification related queries
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        NotificationHelper.createNotificationChannel(this)

        val intent = Intent(this, FlightDetectionService::class.java)
        ContextCompat.startForegroundService(this, intent)

        setContent {
            AirSenseTheme {
                viewModel = viewModel<MainViewModel>()
                flightDetectionAlgorithm = viewModel.flightDetectionAlgorithm
                var selectedItemIndex by remember { mutableIntStateOf(0) }
                val flightState by viewModel.flightState.collectAsState()

                Scaffold(
                    bottomBar = {
                        BottomNavBar(
                            selectedItemIndex = selectedItemIndex,
                            onItemSelected = { index ->
                                selectedItemIndex = index
                                // if (index == 0) finish()
                                if (index == 1) {
                                    val intent = Intent(this, SimulatorActivity::class.java)
                                    startActivity(intent)
                                    viewModel.stopListening()
                                }
                                if (index == 2) {
                                    val intent = Intent(this, SettingsActivity::class.java)
                                    startActivity(intent)
                                    viewModel.stopListening()
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FlightStatusCard(viewModel, flightState, onEndSimulationClick = {
                            if (flightState == FlightState.GROUNDED) viewModel.flightDetectionAlgorithm.forceFlight()
                            else viewModel.flightDetectionAlgorithm.forceLanding()
                        })
                    }
                }

            }
        }
    }

    override fun onResume() {
        Log.d("MainActivity", "onResume")
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.startListening()
        } else {
            Log.e("MainActivity", "viewModel is not initialized yet")
        }
    }
}

@Composable
fun FlightStatusCard(
    viewModel: MainViewModel,
    flightState: FlightState,
    acceleration: Double = 0.3,
    variance: Double = 1.3,
    pressure: Double = 1013.0,
    altitude: Double = 33.0,
    status: String = "grounded", // default status and placeholder
    onEndSimulationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = if (flightState == FlightState.GROUNDED) R.drawable.grounded else R.drawable.cruising),
                        contentDescription = "Grounded/Cruising Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "You are currently ${if (flightState == FlightState.GROUNDED) "on ground" else "flying"}.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text(
                    text = "Accelerometer",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Acceleration", value = "${viewModel.absoluteAcceleration}/s²")
//                InfoRow(label = "Average Variance", value = "$variance m/s³")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text(
                    text = "Barometer",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Pressure", value = "${viewModel.pressure} hPa")
//                InfoRow(label = "Altitude", value = "$altitude m")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEndSimulationClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (flightState == FlightState.GROUNDED) "Force Takeoff" else "Force Landing",
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
