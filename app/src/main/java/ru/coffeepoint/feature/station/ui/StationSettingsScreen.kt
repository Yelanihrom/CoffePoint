package ru.coffeepoint.feature.station.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.station.presentation.StationSettingsViewModel
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SettingCounterRow
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.shared.component.asActionButtonText
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSettingsScreen(
    stationId: Int,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {

    val viewModel: StationSettingsViewModel = viewModel()

    viewModel.loadStation(stationId)

    viewModel.station ?: return

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var selectedTab by remember {
        mutableStateOf(StationSettingsTab.MAIN)
    }

    if (viewModel.isSaving) {
        SyncSendingOverlay()
    }

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = viewModel.syncErrorMessage,
            onDismiss = {
                viewModel.dismissSyncError()
                onBack()
            },
            onRetry = {
                viewModel.saveStation {
                    onBack()
                }
            }
        )
    }

    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Удалить станцию?")
            },
            text = {
                Text("Станция будет удалена без возможности восстановления.")
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
                    Text("Удалить".asActionButtonText())
                }

            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Отмена".asActionButtonText())
                }

            }
        )

    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text("Настройки станции")
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

                    IconButton(
                        onClick = {
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить станцию",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                }
            )

        },
        bottomBar = {

            BottomActionBar(
                text = "Сохранить настройки",
                onClick = {
                    viewModel.saveStation {
                        onBack()
                    }
                }
            )

        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            TabRow(
                selectedTabIndex = selectedTab.ordinal
            ) {

                StationSettingsTab.entries.forEach { tab ->

                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                        },
                        text = {
                            Text(tab.title)
                        }
                    )

                }

            }

            when (selectedTab) {
                StationSettingsTab.MAIN -> MainSettingsSection(
                    viewModel = viewModel
                )

                StationSettingsTab.LIMITS -> LimitsSettingsSection(
                    viewModel = viewModel
                )

                StationSettingsTab.TERMINAL -> TerminalSettingsSection(
                    viewModel = viewModel
                )
            }

        }

    }

}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsSection(
    viewModel: StationSettingsViewModel
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = viewModel.stationName,
                onValueChange = {
                    viewModel.onStationNameChanged(it)
                },
                isError = viewModel.stationNameError,
                supportingText = {
                    if (viewModel.stationNameError) {
                        Text("Введите название станции")
                    }
                },
                label = {
                    Text("Название станции")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.terminalId,
                onValueChange = {
                    viewModel.onTerminalIdChanged(it)
                },
                label = {
                    Text("Номер терминала")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.address,
                onValueChange = {
                    viewModel.onAddressChanged(it)
                },
                label = {
                    Text("Адрес станции")
                },
                modifier = Modifier.fillMaxWidth()
            )

            EmployeeAssignmentDropdown(
                employees = viewModel.employees,
                selectedEmployeeId = viewModel.selectedEmployeeId,
                onEmployeeSelected = viewModel::onEmployeeSelected
            )

            VisitHoursSection(
                visitDays = viewModel.visitDays,
                visitTimeFrom = viewModel.visitTimeFrom,
                visitTimeTo = viewModel.visitTimeTo,
                onDayToggled = viewModel::onVisitDayToggled,
                onTimeFromChanged = viewModel::onVisitTimeFromChanged,
                onTimeToChanged = viewModel::onVisitTimeToChanged
            )

            SettingsCheckboxRow(
                checked = viewModel.excludeFromSupplyPlanning,
                text = "Не учитывать в планирование поставок",
                onCheckedChange = {
                    viewModel.onExcludeFromSupplyPlanningChanged(it)
                }
            )

            SettingsCheckboxRow(
                checked = viewModel.excludeFromOperatorEarnings,
                text = "Не учитывать в заработке оператора",
                onCheckedChange = {
                    viewModel.onExcludeFromOperatorEarningsChanged(it)
                }
            )

        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeAssignmentDropdown(
    employees: List<EmployeeEntity>,
    selectedEmployeeId: Int?,
    onEmployeeSelected: (Int) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }
    val selectedEmployeeName = employees
        .firstOrNull { employee -> employee.id == selectedEmployeeId }
        ?.name
        ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedEmployeeName,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("\u041e\u043f\u0435\u0440\u0430\u0442\u043e\u0440 \u0441\u0442\u0430\u043d\u0446\u0438\u0438")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            employees.forEach { employee ->
                DropdownMenuItem(
                    text = {
                        Text(employee.name)
                    },
                    onClick = {
                        onEmployeeSelected(employee.id)
                        expanded = false
                    }
                )
            }
        }
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VisitHoursSection(
    visitDays: String,
    visitTimeFrom: String,
    visitTimeTo: String,
    onDayToggled: (Int) -> Unit,
    onTimeFromChanged: (String) -> Unit,
    onTimeToChanged: (String) -> Unit
) {

    val context = LocalContext.current
    val selectedDays = remember(visitDays) {
        visitDays.toDaySet()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Часы посещения",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Доступные дни",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            visitDayOptions.forEach { option ->

                FilterChip(
                    selected = selectedDays.contains(option.number),
                    onClick = {
                        onDayToggled(option.number)
                    },
                    label = {
                        Text(option.title)
                    }
                )

            }

        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            VisitTimeButton(
                modifier = Modifier.weight(1f),
                label = "С",
                value = visitTimeFrom,
                onClick = {
                    showVisitTimePicker(
                        context = context,
                        value = visitTimeFrom,
                        onTimeSelected = onTimeFromChanged
                    )
                }
            )

            VisitTimeButton(
                modifier = Modifier.weight(1f),
                label = "До",
                value = visitTimeTo,
                onClick = {
                    showVisitTimePicker(
                        context = context,
                        value = visitTimeTo,
                        onTimeSelected = onTimeToChanged
                    )
                }
            )

        }

    }

}

@Composable
private fun VisitTimeButton(
    modifier: Modifier,
    label: String,
    value: String,
    onClick: () -> Unit
) {

    OutlinedButton(
        modifier = modifier,
        onClick = onClick
    ) {

        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null
        )

        Text(
            text = "$label $value",
            modifier = Modifier.padding(start = 8.dp)
        )

    }

}

@Composable
private fun LimitsSettingsSection(
    viewModel: StationSettingsViewModel
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SettingCounterRow(
                title = "Стаканы",
                value = viewModel.cupsMax,
                onMinus = {
                    viewModel.minusCups()
                },
                onPlus = {
                    viewModel.plusCups()
                }
            )

            SettingCounterRow(
                title = "Крышки",
                value = viewModel.lidsMax,
                onMinus = {
                    viewModel.minusLids()
                },
                onPlus = {
                    viewModel.plusLids()
                }
            )

            SettingCounterRow(
                title = "Кофе",
                value = viewModel.coffeeMax,
                onMinus = {
                    viewModel.minusCoffee()
                },
                onPlus = {
                    viewModel.plusCoffee()
                }
            )

            SettingCounterRow(
                title = "Молоко",
                value = viewModel.milkMax,
                onMinus = {
                    viewModel.minusMilk()
                },
                onPlus = {
                    viewModel.plusMilk()
                }
            )

            SettingCounterRow(
                title = "Шоколад",
                value = viewModel.chocolateMax,
                onMinus = {
                    viewModel.minusChocolate()
                },
                onPlus = {
                    viewModel.plusChocolate()
                }
            )

            SettingCounterRow(
                title = "Топпинг",
                value = viewModel.toppingMax,
                onMinus = {
                    viewModel.minusTopping()
                },
                onPlus = {
                    viewModel.plusTopping()
                }
            )

            SettingCounterRow(
                title = "Сахар",
                value = viewModel.sugarMax,
                onMinus = {
                    viewModel.minusSugar()
                },
                onPlus = {
                    viewModel.plusSugar()
                }
            )

            SettingCounterRow(
                title = "Мешалки",
                value = viewModel.stirrersMax,
                onMinus = {
                    viewModel.minusStirrers()
                },
                onPlus = {
                    viewModel.plusStirrers()
                }
            )

            SettingCounterRow(
                title = "Мусорные пакеты",
                value = viewModel.garbageBagsMax,
                onMinus = {
                    viewModel.minusGarbageBags()
                },
                onPlus = {
                    viewModel.plusGarbageBags()
                }
            )

        }

    }

}

@Composable
private fun TerminalSettingsSection(
    viewModel: StationSettingsViewModel
) {

    val terminalId = viewModel.terminalId.toLongOrNull()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = terminalId
                    ?.let { "Номер терминала: $it" }
                    ?: "Номер терминала не указан",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    viewModel.rebootTerminal()
                },
                enabled = terminalId != null &&
                    !viewModel.isCommandSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Перезагрузить терминал".asActionButtonText())
            }

            OutlinedButton(
                onClick = {
                    viewModel.turnScreenOff()
                },
                enabled = terminalId != null &&
                    !viewModel.isCommandSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выключить экран".asActionButtonText())
            }

            OutlinedButton(
                onClick = {
                    viewModel.turnScreenOn()
                },
                enabled = terminalId != null &&
                    !viewModel.isCommandSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Включить экран".asActionButtonText())
            }

            Text(
                text = "Необходимо, чтобы приостановить продажи",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            viewModel.commandMessage?.let { message ->

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

            }

        }

    }

}

@Composable
private fun SettingsCheckboxRow(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCheckedChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )

    }

}

private fun showVisitTimePicker(
    context: Context,
    value: String,
    onTimeSelected: (String) -> Unit
) {

    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(
                String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )
            )
        },
        hour,
        minute,
        true
    ).show()

}

private fun String.toDaySet(): Set<Int> {
    return split(",")
        .mapNotNull { value -> value.trim().toIntOrNull() }
        .filter { day -> day in 1..7 }
        .toSet()
}

private data class VisitDayOption(
    val number: Int,
    val title: String
)

private val visitDayOptions = listOf(
    VisitDayOption(1, "Пн"),
    VisitDayOption(2, "Вт"),
    VisitDayOption(3, "Ср"),
    VisitDayOption(4, "Чт"),
    VisitDayOption(5, "Пт"),
    VisitDayOption(6, "Сб"),
    VisitDayOption(7, "Вс")
)

private enum class StationSettingsTab(
    val title: String
) {

    MAIN("Основные данные"),
    LIMITS("Лимиты остатков"),
    TERMINAL("Терминал")

}
