package ru.coffeepoint.feature.analytics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.coffeepoint.shared.component.AppMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsMenuScreen(onBack: () -> Unit, onOpenSales: () -> Unit, onOpenServiceHistory: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Аналитика") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(vertical = 8.dp)) {
            AppMenuItem("Продажи", Icons.Default.BarChart, onOpenSales)
            AppMenuItem("История обслуживаний", Icons.Default.History, onOpenServiceHistory)
        }
    }
}
