package ru.coffeepoint.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.coffeepoint.shared.component.AppMenuItem
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenNotifications: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Настройки") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(vertical = 8.dp)) {
            AppMenuItem("Уведомления", Icons.Default.Notifications, onOpenNotifications)
        }
    }
}
