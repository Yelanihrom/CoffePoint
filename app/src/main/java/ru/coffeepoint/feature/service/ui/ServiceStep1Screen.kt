package ru.coffeepoint.feature.service.ui
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.coffeepoint.shared.component.CounterRow
import ru.coffeepoint.feature.station.model.title
import ru.coffeepoint.feature.service.presentation.ServiceStep1ViewModel
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.feature.station.component.StockSection
import ru.coffeepoint.feature.station.component.StockLevelRow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceStep1Screen(
    stationId: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {

    val viewModel: ServiceStep1ViewModel = viewModel()

    viewModel.loadStation(stationId)

    val station = viewModel.station ?: return
    val inventory = viewModel.inventory ?: return

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = station.station.name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Шаг 1 из 2",
                            style = MaterialTheme.typography.bodyMedium
                        )

                    }

                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )

                    }

                }

            )

        },

        bottomBar = {

            BottomActionBar(

                text = "Далее",

                onClick = {

                    viewModel.saveChanges()

                    onNext()

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {


            StockSection(
                title = "Расходники"
            ) {

                CounterRow(
                    title = "Стаканы",
                    value = inventory.cups,
                    max = station.settings.cupsMax,
                    onMinus = {
                        viewModel.minusCups()
                    },
                    onPlus = {
                        viewModel.plusCups()
                    }
                )

                CounterRow(
                    title = "Крышки",
                    value = inventory.lids,
                    max = station.settings.lidsMax,
                    onMinus = {
                        viewModel.minusLids()
                    },
                    onPlus = {
                        viewModel.plusLids()
                    }
                )

                StockLevelRow(
                    title = "Сахар",
                    value = inventory.sugar.title(),
                    onMinus = {
                        viewModel.previousSugar()
                    },
                    onPlus = {
                        viewModel.nextSugar()
                    }
                )

                StockLevelRow(
                    title = "Мешалки",
                    value = inventory.stirrers.title(),
                    onMinus = {
                        viewModel.previousStirrers()
                    },
                    onPlus = {
                        viewModel.nextStirrers()
                    }
                )
            }
            StockSection(
                title = "Ингредиенты"
            ) {

                CounterRow(
                    title = "Кофе",
                    value = inventory.coffee,
                    max = station.settings.coffeeMax,
                    onMinus = {
                        viewModel.minusCoffee()
                    },
                    onPlus = {
                        viewModel.plusCoffee()
                    }
                )

                CounterRow(
                    title = "Молоко",
                    value = inventory.milk,
                    max = station.settings.milkMax,
                    onMinus = {
                        viewModel.minusMilk()
                    },
                    onPlus = {
                        viewModel.plusMilk()
                    }
                )

                CounterRow(
                    title = "Шоколад",
                    value = inventory.chocolate,
                    max = station.settings.chocolateMax,
                    onMinus = {
                        viewModel.minusChocolate()
                    },
                    onPlus = {
                        viewModel.plusChocolate()
                    }
                )

                CounterRow(
                    title = "Топпинг",
                    value = inventory.topping,
                    max = station.settings.toppingMax,
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
