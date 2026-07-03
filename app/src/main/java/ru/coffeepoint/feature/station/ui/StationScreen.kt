package ru.coffeepoint.feature.station.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.feature.service.component.PhotoSection
import ru.coffeepoint.feature.station.component.SalesCard
import ru.coffeepoint.feature.station.component.StockCard
import ru.coffeepoint.feature.station.model.StationTab
import ru.coffeepoint.feature.station.presentation.StationViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen(
    stationId: Int,
    onBack: () -> Unit,
    onStartService: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {

    val viewModel: StationViewModel = viewModel()

    viewModel.loadStation(stationId)

    val detail = viewModel.station ?: return

    var selectedTab by remember {
        mutableStateOf(StationTab.SALES)
    }

    var showMenu by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(detail.station.name)

                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )

                    }

                },

                actions = {

                    DropdownMenu(

                        expanded = showMenu,

                        onDismissRequest = {

                            showMenu = false

                        }

                    ) {

                        DropdownMenuItem(

                            text = {

                                Text("Редактировать")

                            },

                            onClick = {

                                showMenu = false

                                onEdit()

                            }

                        )

                        DropdownMenuItem(

                            text = {

                                Text("Удалить станцию")

                            },

                            onClick = {

                                showMenu = false

                                showDeleteDialog = true

                            }

                        )

                    }

                    IconButton(

                        onClick = {

                            showMenu = true

                        }

                    ) {

                        Icon(

                            Icons.Default.MoreVert,

                            contentDescription = null

                        )

                    }

                },

                colors = TopAppBarDefaults.topAppBarColors()

            )

        },

        bottomBar = {

            BottomActionBar(
                text = "Начать обслуживание",
                onClick = onStartService
            )

            if (showDeleteDialog) {

                AlertDialog(

                    onDismissRequest = {

                        showDeleteDialog = false

                    },

                    title = {

                        Text("Удалить станцию?")

                    },

                    text = {

                        Text(
                            "Станция будет удалена без возможности восстановления."
                        )

                    },

                    confirmButton = {

                        TextButton(

                            onClick = {

                                showDeleteDialog = false

                                viewModel.deleteStation {

                                    onDeleted()

                                }

                            }

                        ) {

                            Text("Удалить")

                        }

                    },

                    dismissButton = {

                        TextButton(

                            onClick = {

                                showDeleteDialog = false

                            }

                        ) {

                            Text("Отмена")

                        }

                    }

                )

            }
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {

            detail.station.terminalId?.let { terminalId ->

                Text(

                    text = "Терминал: $terminalId",

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )

                )

            }

            TabRow(
                selectedTabIndex = selectedTab.ordinal
            ) {

                Tab(
                    selected = selectedTab == StationTab.SALES,
                    onClick = {
                        selectedTab = StationTab.SALES
                    },
                    text = {
                        Text("Продажи")
                    }
                )

                Tab(
                    selected = selectedTab == StationTab.STOCK,
                    onClick = {
                        selectedTab = StationTab.STOCK
                    },
                    text = {
                        Text("Остатки")
                    }
                )

            }
            LazyColumn(

                modifier = Modifier.weight(1f)

            ) {

                item {

                    when (selectedTab) {

                        StationTab.SALES -> {

                            SalesCard(
                                detail = detail
                            )

                            PhotoSection(
                                machinePhotoPath = detail.station.machinePhotoPath,
                                overviewPhotoPath = detail.station.overviewPhotoPath
                            )

                        }

                        StationTab.STOCK -> {

                            StockCard(
                                detail = detail
                            )

                        }

                    }

                }

            }
        }

    }

}