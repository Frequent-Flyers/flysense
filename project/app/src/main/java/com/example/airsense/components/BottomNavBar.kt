package com.example.airsense

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavBar(
    selectedItemIndex: Int,
    onItemSelected: (Int) -> Unit
) {
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

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = { onItemSelected(index) },
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
