package com.example.airsense

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airsense.data.PreferenceHelper
import com.example.airsense.detector.algorithm.FlightDetectionAlgorithm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PreferenceHelper,
    private val flightDetectionAlgorithm: FlightDetectionAlgorithm,
) : ViewModel() {
    private val _fdsMode = MutableStateFlow("Primary")
    val fdsMode: StateFlow<String> = _fdsMode.asStateFlow()

    private val _overrideThemeMode = MutableStateFlow(false)
    val overrideThemeMode: StateFlow<Boolean> = _overrideThemeMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _canNotify = MutableStateFlow(true)
    val canNotify: StateFlow<Boolean> = _canNotify.asStateFlow()


    init {
        viewModelScope.launch {
            repository.fdsMode.collect { _fdsMode.value = it }
        }
        viewModelScope.launch {
            repository.overrideThemeMode.collect { _overrideThemeMode.value = it }
        }
        viewModelScope.launch {
            repository.isDarkTheme.collect { _isDarkTheme.value = it }
        }
        viewModelScope.launch {
            repository.canNotify.collect { _canNotify.value = it }
        }
    }

    fun setFDSMode(mode: String) {
        _fdsMode.value = mode
        viewModelScope.launch {
            repository.setFDSMode(mode)
        }

        flightDetectionAlgorithm.setDetectionMode(mode)

        Log.d("SettingsViewModel", "FDS Mode set to: $mode")
        Log.d("SettingsViewModel", "Current Mode from Algorithm: ${flightDetectionAlgorithm.getDetectionMode()}")
    }


    fun setOverrideThemeMode(override: Boolean) {
        _overrideThemeMode.value = override
        viewModelScope.launch {
            repository.setOverrideThemeMode(override)
        }
    }

    fun setCanNotify(canNotify: Boolean) {
        _canNotify.value = canNotify
        viewModelScope.launch {
            repository.setCanNotify(canNotify)
        }
    }
}