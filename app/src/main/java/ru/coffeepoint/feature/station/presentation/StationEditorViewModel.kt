package ru.coffeepoint.feature.station.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.station.model.StockLevel

class StationEditorViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    private var editingStation: StationDetail? = null

    var stationName by mutableStateOf("")
        private set

    var stationNameError by mutableStateOf(false)
        private set

    var terminalId by mutableStateOf("")
        private set

    var cupsMax by mutableStateOf(15)
        private set

    var lidsMax by mutableStateOf(6)
        private set

    var coffeeMax by mutableStateOf(6)
        private set

    var milkMax by mutableStateOf(10)
        private set

    var chocolateMax by mutableStateOf(10)
        private set

    var toppingMax by mutableStateOf(10)
        private set

    fun onStationNameChanged(value: String) {

        stationName = value

        stationNameError = false

    }

    fun onTerminalIdChanged(value: String) {

        terminalId = value.filter { it.isDigit() }

    }

    fun loadStation(
        stationId: Int
    ) {

        if (stationId == 0) {

            editingStation = null

            stationName = ""
            terminalId = ""

            cupsMax = 15
            lidsMax = 6
            coffeeMax = 6
            milkMax = 10
            chocolateMax = 10
            toppingMax = 10

            return

        }

        if (editingStation?.station?.id == stationId) return

        viewModelScope.launch {

            val detail = repository.getStation(stationId) ?: return@launch

            editingStation = detail

            stationName = detail.station.name
            terminalId = detail.station.terminalId?.toString() ?: ""

            cupsMax = detail.settings.cupsMax
            lidsMax = detail.settings.lidsMax
            coffeeMax = detail.settings.coffeeMax
            milkMax = detail.settings.milkMax
            chocolateMax = detail.settings.chocolateMax
            toppingMax = detail.settings.toppingMax

        }

    }

    fun plusCups() {
        cupsMax = increment(cupsMax)
    }

    fun minusCups() {
        cupsMax = decrement(cupsMax)
    }

    fun plusLids() {
        lidsMax = increment(lidsMax)
    }

    fun minusLids() {
        lidsMax = decrement(lidsMax)
    }

    fun plusCoffee() {
        coffeeMax = increment(coffeeMax)
    }

    fun minusCoffee() {
        coffeeMax = decrement(coffeeMax)
    }

    fun plusMilk() {
        milkMax = increment(milkMax)
    }

    fun minusMilk() {
        milkMax = decrement(milkMax)
    }

    fun plusChocolate() {
        chocolateMax = increment(chocolateMax)
    }

    fun minusChocolate() {
        chocolateMax = decrement(chocolateMax)
    }

    fun plusTopping() {
        toppingMax = increment(toppingMax)
    }

    fun minusTopping() {
        toppingMax = decrement(toppingMax)
    }

    private fun increment(
        current: Int
    ): Int {

        return (current + 1).coerceAtMost(99)

    }

    private fun decrement(
        current: Int
    ): Int {

        return (current - 1).coerceAtLeast(1)

    }

    fun saveStation(
        onSuccess: () -> Unit
    ) {

        stationNameError = stationName.isBlank()

        if (stationNameError) {
            return
        }

        val current = editingStation
        val detail = if (current == null) {

            StationDetail(

                station = CoffeeStation(

                    id = 0,
                    name = stationName,
                    terminalId = terminalId.toLongOrNull(),
                    lastServiceDate = "",
                    salesAfterService = 0

                ),

                inventory = StationInventory(

                    cups = cupsMax,
                    lids = lidsMax,
                    coffee = coffeeMax,
                    milk = milkMax,
                    chocolate = chocolateMax,
                    topping = toppingMax,

                    sugar = StockLevel.FULL,
                    stirrers = StockLevel.FULL

                ),

                settings = StationSettings(

                    cupsMax = cupsMax,
                    lidsMax = lidsMax,
                    coffeeMax = coffeeMax,
                    milkMax = milkMax,
                    chocolateMax = chocolateMax,
                    toppingMax = toppingMax

                )

            )

        } else {

            current.copy(

                station = current.station.copy(

                    name = stationName,
                    terminalId = terminalId.toLongOrNull()

                ),

                settings = current.settings.copy(

                    cupsMax = cupsMax,
                    lidsMax = lidsMax,
                    coffeeMax = coffeeMax,
                    milkMax = milkMax,
                    chocolateMax = chocolateMax,
                    toppingMax = toppingMax

                )

            )

        }

        viewModelScope.launch {

            if (current == null) {

                repository.addStation(detail)

            } else {

                repository.saveStation(detail)

            }

            onSuccess()

        }

    }

}
