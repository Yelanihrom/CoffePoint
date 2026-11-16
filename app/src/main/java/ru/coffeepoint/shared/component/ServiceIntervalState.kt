package ru.coffeepoint.shared.component

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import ru.coffeepoint.core.data.preferences.ServiceIntervalPreferences
import ru.coffeepoint.feature.station.model.ServiceIntervals

@Composable
fun rememberServiceIntervals(): ServiceIntervals {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { ServiceIntervalPreferences(context) }
    val flow = remember(store) { store.observe() }
    return flow.collectAsState(initial = remember(store) { store.read() }).value
}
