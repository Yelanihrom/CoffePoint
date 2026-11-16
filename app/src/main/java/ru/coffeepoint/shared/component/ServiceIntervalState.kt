package ru.coffeepoint.shared.component

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import ru.coffeepoint.core.data.repository.ServiceIntervalRepository
import ru.coffeepoint.feature.station.model.ServiceIntervals

@Composable
fun rememberServiceIntervals(repository: ServiceIntervalRepository? = null): ServiceIntervals {
    val context = LocalContext.current.applicationContext
    val store = remember(context, repository) { repository ?: ServiceIntervalRepository(context) }
    val flow = remember(store) { store.observe() }
    return flow.collectAsState(initial = ServiceIntervals()).value
}
