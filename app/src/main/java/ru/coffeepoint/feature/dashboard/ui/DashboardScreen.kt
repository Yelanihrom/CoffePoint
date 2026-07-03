package ru.coffeepoint.feature.dashboard.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import ru.coffeepoint.feature.dashboard.component.StationCard
import ru.coffeepoint.feature.dashboard.presentation.DashboardViewModel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton


@Composable
fun DashboardScreen(
    onStationClick: (Int) -> Unit,
    onAddStation: () -> Unit
) {

    val viewModel: DashboardViewModel = viewModel()

    LaunchedEffect(Unit) {

        viewModel.loadStations()

    }

    val stations = viewModel.stations
    val context = LocalContext.current
    var refreshing by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    LaunchedEffect(refreshing) {

        if (refreshing) {

            delay(1500)

            refreshing = false

        }

    }

    Scaffold(

        topBar = {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Список кофе-станций",
                    style = MaterialTheme.typography.headlineSmall
                )

                IconButton(
                    onClick = {

                        refreshing = true

                        viewModel.refreshSales()

                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        modifier = Modifier.rotate(
                            if (refreshing) rotation else 0f
                        )
                    )

                }

            }

        },
        floatingActionButton = {

            FloatingActionButton(

                onClick = onAddStation

            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить станцию"
                )

            }

        }

    ) { padding ->

        LazyColumn(

            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)

        ) {

            items(stations) { detail ->

                StationCard(
                    station = detail.station,
                    onClick = {
                        onStationClick(detail.station.id)
                    }
                )

            }

        }

    }

}