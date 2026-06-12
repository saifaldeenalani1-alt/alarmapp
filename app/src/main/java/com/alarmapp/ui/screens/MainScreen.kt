package com.alarmapp.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class TabItem(val title: String, val icon: ImageVector, val screen: @Composable () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkTheme: Boolean = false,
    onThemeChanged: (Boolean) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(1) }

    val tabs = remember {
        listOf(
            TabItem("منبه", Icons.Default.Alarm) { AlarmScreen() },
            TabItem("ساعة", Icons.Default.AccessTime) { ClockScreen() },
            TabItem("مؤقت", Icons.Default.Timer) { TimerScreen() },
            TabItem("أيام", Icons.Default.CalendarMonth) { DayCounterScreen() },
            TabItem("إعدادات", Icons.Default.Settings) { SettingsScreen(
                isDarkTheme = isDarkTheme,
                onThemeChanged = onThemeChanged
            ) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabs[selectedTab].title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            tabs[selectedTab].screen()
        }
    }
}
