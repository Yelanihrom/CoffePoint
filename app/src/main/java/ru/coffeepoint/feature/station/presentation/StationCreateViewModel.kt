package ru.coffeepoint.feature.station.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.util.normalizeTime
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage

class StationCreateViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    private var editingStation: StationDetail? = null
    private var savedStationId: Int? = null

    var stationName by mutableStateOf("")
        private set

    var stationNameError by mutableStateOf(false)
        private set

    var terminalId by mutableStateOf("")
        private set

    var address by mutableStateOf("")
        private set

    var visitDays by mutableStateOf("1,2,3,4,5")
        private set

    var visitTimeFrom by mutableStateOf("09:00")
        private set

    var visitTimeTo by mutableStateOf("18:00")
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

    var sugarMax by mutableStateOf(2)
        private set

    var stirrersMax by mutableStateOf(4)
        private set

    var garbageBagsMax by mutableStateOf(2)
        private set

    var excludeFromSupplyPlanning by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    fun onStationNameChanged(value: String) {

        stationName = value

        stationNameError = false

    }

    fun onTerminalIdChanged(value: String) {

        terminalId = value.filter { it.isDigit() }

    }

    fun onAddressChanged(value: String) {

        address = value

    }

    fun onVisitDayToggled(day: Int) {

        val days = visitDays.toDaySet().toMutableSet()

        if (days.contains(day)) {
            days.remove(day)
        } else {
            days.add(day)
        }

        visitDays = days.sorted().joinToString(",")

    }

    fun onVisitTimeFromChanged(value: String) {

        visitTimeFrom = normalizeTime(value).ifBlank { "09:00" }

    }

    fun onVisitTimeToChanged(value: String) {

        visitTimeTo = normalizeTime(value).ifBlank { "18:00" }

    }

    fun onExcludeFromSupplyPlanningChanged(value: Boolean) {

        excludeFromSupplyPlanning = value

    }

    fun loadStation(
        stationId: Int
    ) {

        if (stationId == 0) {

            if (savedStationId != null) return

            editingStation = null

            stationName = ""
            terminalId = ""
            address = ""
            visitDays = "1,2,3,4,5"
            visitTimeFrom = "09:00"
            visitTimeTo = "18:00"

            cupsMax = 15
            lidsMax = 6
            coffeeMax = 6
            milkMax = 10
            chocolateMax = 10
            toppingMax = 10
            sugarMax = 2
            stirrersMax = 4
            garbageBagsMax = 2
            excludeFromSupplyPlanning = false

            return

        }

        if (editingStation?.station?.id == stationId) return

        viewModelScope.launch {

            val detail = repository.getStation(stationId) ?: return@launch

            editingStation = detail

            stationName = detail.station.name
            terminalId = detail.station.terminalId?.toString() ?: ""
            address = detail.station.address
            visitDays = detail.station.visitDays
            visitTimeFrom = normalizeTime(detail.station.visitTimeFrom)
                .ifBlank { "09:00" }
            visitTimeTo = normalizeTime(detail.station.visitTimeTo)
                .ifBlank { "18:00" }
            excludeFromSupplyPlanning =
                detail.station.excludeFromSupplyPlanning

            cupsMax = detail.settings.cupsMax
            lidsMax = detail.settings.lidsMax
            coffeeMax = detail.settings.coffeeMax
            milkMax = detail.settings.milkMax
            chocolateMax = detail.settings.chocolateMax
            toppingMax = detail.settings.toppingMax
            sugarMax = detail.settings.sugarMax
            stirrersMax = detail.settings.stirrersMax
            garbageBagsMax = detail.settings.garbageBagsMax

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

    fun plusSugar() {
        sugarMax = increment(sugarMax)
    }

    fun minusSugar() {
        sugarMax = decrement(sugarMax)
    }

    fun plusStirrers() {
        stirrersMax = increment(stirrersMax)
    }

    fun minusStirrers() {
        stirrersMax = decrement(stirrersMax)
    }

    fun plusGarbageBags() {
        garbageBagsMax = increment(garbageBagsMax)
    }

    fun minusGarbageBags() {
        garbageBagsMax = decrement(garbageBagsMax)
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

        if (isSaving) return

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
                    address = address,
                    visitDays = visitDays,
                    visitTimeFrom = visitTimeFrom,
                    visitTimeTo = visitTimeTo,
                    excludeFromSupplyPlanning = excludeFromSupplyPlanning,
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

                    sugar = sugarMax,
                    stirrers = stirrersMax,
                    garbageBags = garbageBagsMax

                ),

                settings = StationSettings(

                    cupsMax = cupsMax,
                    lidsMax = lidsMax,
                    coffeeMax = coffeeMax,
                    milkMax = milkMax,
                    chocolateMax = chocolateMax,
                    toppingMax = toppingMax,
                    sugarMax = sugarMax,
                    stirrersMax = stirrersMax,
                    garbageBagsMax = garbageBagsMax

                )

            )

        } else {

            current.copy(

                station = current.station.copy(

                    name = stationName,
                    terminalId = terminalId.toLongOrNull(),
                    address = address,
                    visitDays = visitDays,
                    visitTimeFrom = visitTimeFrom,
                    visitTimeTo = visitTimeTo,
                    excludeFromSupplyPlanning = excludeFromSupplyPlanning

                ),

                settings = current.settings.copy(

                    cupsMax = cupsMax,
                    lidsMax = lidsMax,
                    coffeeMax = coffeeMax,
                    milkMax = milkMax,
                    chocolateMax = chocolateMax,
                    toppingMax = toppingMax,
                    sugarMax = sugarMax,
                    stirrersMax = stirrersMax,
                    garbageBagsMax = garbageBagsMax

                )

            )

        }

        viewModelScope.launch {

            isSaving = true
            showSyncError = false

            val synced = if (savedStationId != null) {

                repository.syncStationNow(savedStationId!!)

            } else if (current == null) {

                val result = repository.addStation(detail)
                savedStationId = result.stationId
                editingStation = repository.getStation(result.stationId)
                result.synced

            } else {

                repository.saveStation(
                    detail = detail,
                    syncImmediately = true
                )

            }

            isSaving = false

            if (synced) {
                onSuccess()
            } else {
                syncErrorMessage = savedStationId
                    ?.let { stationId -> repository.getStation(stationId) }
                    ?.syncErrorDialogMessage()
                    ?: "Проверьте подключение к интернету или VPN"
                showSyncError = true
            }

        }

    }

    fun dismissSyncError() {
        showSyncError = false
    }

    fun retrySync(
        onSuccess: () -> Unit
    ) {
        saveStation(onSuccess)
    }

}

private fun String.toDaySet(): Set<Int> {
    return split(",")
        .mapNotNull { value -> value.trim().toIntOrNull() }
        .filter { day -> day in 1..7 }
        .toSet()
}
