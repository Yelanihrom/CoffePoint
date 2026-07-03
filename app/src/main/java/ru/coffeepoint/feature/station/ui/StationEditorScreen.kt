package ru.coffeepoint.feature.station.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import ru.coffeepoint.shared.component.SettingCounterRow
import ru.coffeepoint.shared.component.BottomActionBar
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.station.presentation.StationEditorViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationCreateScreen(
    stationId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: StationEditorViewModel = viewModel()

    viewModel.loadStation(stationId)

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
                }

            }

        }

    }

}