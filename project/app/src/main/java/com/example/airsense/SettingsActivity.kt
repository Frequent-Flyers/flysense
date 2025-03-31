package com.example.airsense

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.compose.AirSenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AirSenseTheme { 
                var selectedItemIndex by remember { mutableStateOf(2) }

                Scaffold(
                    bottomBar = {
                        BottomNavBar(
                            selectedItemIndex = selectedItemIndex,
                            onItemSelected = { index ->
                                selectedItemIndex = index
                                when (index) {
                                    0 -> startActivity(Intent(this, MainActivity::class.java))
                                    1 -> startActivity(Intent(this, SimulatorActivity::class.java))
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
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val fdsMode by viewModel.fdsMode.collectAsState()
    val overrideThemeMode by viewModel.overrideThemeMode.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val canNotify by viewModel.canNotify.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ListItem(
            headlineContent = { Text("Dark Mode") },
            supportingContent = {
                Text("Overrides the system setting, permanently enabling dark mode.")
            },
            trailingContent = {
                Switch(
                    checked = overrideThemeMode,
                    onCheckedChange = { isChecked ->
                        if (isChecked) viewModel.setOverrideThemeMode(true)
                        else viewModel.setOverrideThemeMode(false)
                    }
                )
            }
        )
        
        ListItem(
            headlineContent = { Text("Notify") },
            supportingContent = {
                Text("Enables or disables notifications when a new flight state is detected.")
            },
            trailingContent = {
                Switch(
                    checked = canNotify,
                    onCheckedChange = { isChecked ->
                        if (isChecked) viewModel.setCanNotify(true)
                        else viewModel.setCanNotify(false)
                    }
                )
            }
        )
        
        ListItem(
            headlineContent = { Text("FDS Mode") },
            supportingContent = {
                Column {
                    Text("Controls the detection system method.")
                    Spacer(modifier = Modifier.height(8.dp))
                    FDSSelector(
                        selectedFDS = fdsMode,
                        onFDSSelected = { viewModel.setFDSMode(it) }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FDSSelector(selectedFDS: String, onFDSSelected: (String) -> Unit) {
    val options = listOf("Primary", "A", "B")
    
    SingleChoiceSegmentedButtonRow { 
        options.forEach { option ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = options.indexOf(option),
                    count = options.size
                ),
                onClick = { onFDSSelected(option) },
                selected = option == selectedFDS,
                label = { Text(option) }
            ) 
        }
    }
}