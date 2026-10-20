package ru.coffeepoint.feature.station.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import ru.coffeepoint.shared.component.SettingCounterRow
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.station.presentation.StationCreateViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationCreateScreen(
    stationId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: StationCreateViewModel = viewModel()

    viewModel.loadStation(stationId)

    if (viewModel.isSaving) {
        SyncSendingOverlay()
    }

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = viewModel.syncErrorMessage,
            onDismiss = {
                viewModel.dismissSyncError()
                onSaved()
            },
            onRetry = {
                viewModel.retrySync {
                    onSaved()
                }
            }
        )
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(

                        if (stationId == 0)
                            "Новая станция"
                        else
                            "Редактирование станции"

                    )

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

                }

            )

        },
        bottomBar = {

            BottomActionBar(

                text = "Сохранить",

                onClick = {

                    viewModel.saveStation {

                        onSaved()

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

            verticalArrangement = Arrangement.spacedBy(24.dp)

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

            EditorVisitHoursSection(
                visitDays = viewModel.visitDays,
                visitTimeFrom = viewModel.visitTimeFrom,
                visitTimeTo = viewModel.visitTimeTo,
                onDayToggled = viewModel::onVisitDayToggled,
                onTimeFromChanged = viewModel::onVisitTimeFromChanged,
                onTimeToChanged = viewModel::onVisitTimeToChanged
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onExcludeFromSupplyPlanningChanged(
                            !viewModel.excludeFromSupplyPlanning
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = viewModel.excludeFromSupplyPlanning,
                    onCheckedChange = {
                        viewModel.onExcludeFromSupplyPlanningChanged(it)
                    }
                )

                Text(
                    text = "Не учитывать в планирование поставок",
                    style = MaterialTheme.typography.bodyMedium
                )

            }

            Card {

                Column(

                    modifier = Modifier.padding(16.dp),

                    verticalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.titleLarge
                    )
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

                        title = "Пакеты",

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

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorVisitHoursSection(
    visitDays: String,
    visitTimeFrom: String,
    visitTimeTo: String,
    onDayToggled: (Int) -> Unit,
    onTimeFromChanged: (String) -> Unit,
    onTimeToChanged: (String) -> Unit
) {

    val context = LocalContext.current
    val selectedDays = remember(visitDays) {
        visitDays.toEditorDaySet()
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
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

                editorVisitDayOptions.forEach { option ->

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

                EditorVisitTimeButton(
                    modifier = Modifier.weight(1f),
                    label = "С",
                    value = visitTimeFrom,
                    onClick = {
                        showEditorVisitTimePicker(
                            context = context,
                            value = visitTimeFrom,
                            onTimeSelected = onTimeFromChanged
                        )
                    }
                )

                EditorVisitTimeButton(
                    modifier = Modifier.weight(1f),
                    label = "До",
                    value = visitTimeTo,
                    onClick = {
                        showEditorVisitTimePicker(
                            context = context,
                            value = visitTimeTo,
                            onTimeSelected = onTimeToChanged
                        )
                    }
                )

            }

        }

    }

}

@Composable
private fun EditorVisitTimeButton(
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

private fun showEditorVisitTimePicker(
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

private fun String.toEditorDaySet(): Set<Int> {
    return split(",")
        .mapNotNull { value -> value.trim().toIntOrNull() }
        .filter { day -> day in 1..7 }
        .toSet()
}

private data class EditorVisitDayOption(
    val number: Int,
    val title: String
)

private val editorVisitDayOptions = listOf(
    EditorVisitDayOption(1, "Пн"),
    EditorVisitDayOption(2, "Вт"),
    EditorVisitDayOption(3, "Ср"),
    EditorVisitDayOption(4, "Чт"),
    EditorVisitDayOption(5, "Пт"),
    EditorVisitDayOption(6, "Сб"),
    EditorVisitDayOption(7, "Вс")
)
