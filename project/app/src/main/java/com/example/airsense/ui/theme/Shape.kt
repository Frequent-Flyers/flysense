package com.example.airsense.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),   // Small components like buttons, chips
    medium = RoundedCornerShape(8.dp), // Medium components like cards, dialogs
    large = RoundedCornerShape(16.dp)  // Large components like modal sheets
)
