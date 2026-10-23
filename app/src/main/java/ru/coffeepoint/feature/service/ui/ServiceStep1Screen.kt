package ru.coffeepoint.feature.service.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.coffeepoint.R
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.shared.component.CounterRow
import ru.coffeepoint.feature.service.presentation.ServiceStep1ViewModel
import ru.coffeepoint.feature.service.presentation.WarehouseWriteOffSource
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.shared.component.savedLocallySyncMessage
import ru.coffeepoint.feature.station.component.StockSection


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceStep1Screen(
    stationId: Int,
    role: UserRole,
    onBack: () -> Unit,
    onNext: () -> Unit,
    stepLabel: String = "Шаг 1 из 2",
    actionText: String = "Далее",
    syncImmediatelyOnSave: Boolean = false,
    isSupplyServiceFlow: Boolean = false
) {

    val viewModel: ServiceStep1ViewModel = viewModel()

    viewModel.loadStation(stationId)

    val station = viewModel.station ?: return
    val inventory = viewModel.inventory ?: return
    val showMaxValues = role != UserRole.OPERATOR
    val effectiveStepLabel = if (isSupplyServiceFlow) {
        "Шаг 1 из 3"
    } else {
        stepLabel
    }
    val continueAfterSave = onNext
    val effectiveActionText = actionText

    if (viewModel.isSaving) {
        SyncSendingOverlay()
    }

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = savedLocallySyncMessage(viewModel.syncErrorMessage),
            dismissText = "\u0413\u043e\u0442\u043e\u0432\u043e",
            retryText = "\u041f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u044c \u0441\u0435\u0439\u0447\u0430\u0441",
            onDismiss = {
                viewModel.dismissSyncError()
                continueAfterSave()
            },
            onRetry = {
                viewModel.retrySave(
                    syncImmediately = syncImmediatelyOnSave
                ) {
                    continueAfterSave()
                }
            }
        )
    }

    if (viewModel.showWarehouseWriteOffConfirm) {
        val sourceExpanded = remember { mutableStateOf(false) }

        AlertDialog(
            modifier = Modifier.widthIn(min = 320.dp),
            onDismissRequest = {
                viewModel.dismissWarehouseWriteOffConfirm()
            },
            title = {
                Text("\u041e\u0442\u043a\u0443\u0434\u0430 \u0441\u043f\u0438\u0441\u0430\u0442\u044c \u0442\u043e\u0432\u0430\u0440?")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043e\u0442\u043a\u0443\u0434\u0430 \u043d\u0443\u0436\u043d\u043e \u0441\u043f\u0438\u0441\u0430\u0442\u044c \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b")

                    ExposedDropdownMenuBox(
                        expanded = sourceExpanded.value,
                        onExpandedChange = {
                            sourceExpanded.value = !sourceExpanded.value
                        }
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedWriteOffSource.title,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("\u0418\u0441\u0442\u043e\u0447\u043d\u0438\u043a")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = sourceExpanded.value
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = sourceExpanded.value,
                            onDismissRequest = {
                                sourceExpanded.value = false
                            }
                        ) {
                            WarehouseWriteOffSource.entries.forEach { source ->
                                DropdownMenuItem(
                                    text = {
                                        Text(source.title)
                                    },
                                    onClick = {
                                        viewModel.selectWriteOffSource(source)
                                        sourceExpanded.value = false
                                    }
                                )
                            }
                        }
                    }

                    Text("\u0411\u0443\u0434\u0435\u0442 \u0441\u043f\u0438\u0441\u0430\u043d\u043e:")

                    viewModel.warehouseWriteOffItems.forEach { item ->
                        val showWarehouseAvailability =
                            viewModel.selectedWriteOffSource == WarehouseWriteOffSource.Warehouse
                        val amountColor = when {
                            !showWarehouseAvailability -> MaterialTheme.colorScheme.onSurface
                            item.amount > item.available -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(item.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = item.title,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = item.amount.toString(),
                                color = amountColor
                            )

                            if (showWarehouseAvailability) {
                                Text(
                                    text = " / ${item.available}"
                                )
                            }
                        }
                    }

                    if (viewModel.warehouseWriteOffErrorMessage.isNotBlank()) {
                        Text(
                            text = "\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u043e\u0441\u0442\u0430\u0442\u043a\u043e\u0432 \u043d\u0430 \u0441\u043a\u043b\u0430\u0434\u0435",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.dismissWarehouseWriteOffConfirm()
                        }
                    ) {
                        Text("\u041e\u0442\u043c\u0435\u043d\u0430")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.confirmWarehouseWriteOff(
                                syncImmediately = syncImmediatelyOnSave
                            ) {
                                continueAfterSave()
                            }
                        }
                    ) {
                        Text("\u0421\u043f\u0438\u0441\u0430\u0442\u044c")
                    }
                }
            },
            dismissButton = {}
        )
    }

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
                            text = effectiveStepLabel,
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

                text = effectiveActionText,

                onClick = {

                    viewModel.requestSave(
                        isSupplyServiceFlow = isSupplyServiceFlow,
                        syncImmediately = syncImmediatelyOnSave
                    ) {
                        continueAfterSave()
                    }

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
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_cups,
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
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_lids,
                    onMinus = {
                        viewModel.minusLids()
                    },
                    onPlus = {
                        viewModel.plusLids()
                    }
                )

                CounterRow(
                    title = "Сахар",
                    value = inventory.sugar,
                    max = station.settings.sugarMax,
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_sugar_cubes,
                    onMinus = {
                        viewModel.minusSugar()
                    },
                    onPlus = {
                        viewModel.plusSugar()
                    }
                )

                CounterRow(
                    title = "Мешалки",
                    value = inventory.stirrers,
                    max = station.settings.stirrersMax,
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_stirrers,
                    onMinus = {
                        viewModel.minusStirrers()
                    },
                    onPlus = {
                        viewModel.plusStirrers()
                    }
                )

                CounterRow(
                    title = "Пакеты",
                    value = inventory.garbageBags,
                    max = station.settings.garbageBagsMax,
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_garbage_bags,
                    onMinus = {
                        viewModel.minusGarbageBags()
                    },
                    onPlus = {
                        viewModel.plusGarbageBags()
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
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_coffee_bean,
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
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_milk,
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
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_chocolate,
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
                    showMax = showMaxValues,
                    iconRes = R.drawable.ic_supply_topping_banana,
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
