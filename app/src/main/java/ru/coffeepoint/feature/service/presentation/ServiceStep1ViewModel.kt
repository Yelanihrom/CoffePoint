package ru.coffeepoint.feature.service.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.data.repository.WarehouseStockWriteOff
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncMessages
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

class ServiceStep1ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)
    private val warehouseRepository = WarehouseStockRepository(application)

    var station by mutableStateOf<StationDetail?>(null)
        private set

    var inventory by mutableStateOf<StationInventory?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf(DEFAULT_SYNC_ERROR_MESSAGE)
        private set

    var showWarehouseWriteOffConfirm by mutableStateOf(false)
        private set

    var warehouseWriteOffItems by mutableStateOf<List<WarehouseWriteOffUiItem>>(emptyList())
        private set

    var selectedWriteOffSource by mutableStateOf(WarehouseWriteOffSource.Warehouse)
        private set

    var warehouseWriteOffErrorMessage by mutableStateOf("")
        private set

    private var pendingWarehouseWriteOffItems: List<WarehouseWriteOffUiItem> = emptyList()
    private var warehouseWriteOffApplied = false
    private var pendingWriteOffSource = WarehouseWriteOffSource.Warehouse

    fun loadStation(stationId: Int) {
        if (station != null) return

        viewModelScope.launch {
            val result = repository.getStation(stationId) ?: return@launch

            station = result
            inventory = result.inventory.copy()
        }
    }

    fun plusCups() {
        val current = inventory ?: return
        val max = station?.settings?.cupsMax ?: return

        inventory = current.copy(
            cups = increment(
                current.cups,
                max
            )
        )
    }

    fun minusCups() {
        val current = inventory ?: return

        inventory = current.copy(
            cups = decrement(current.cups)
        )
    }

    fun plusLids() {
        val current = inventory ?: return
        val max = station?.settings?.lidsMax ?: return

        inventory = current.copy(
            lids = increment(
                current.lids,
                max
            )
        )
    }

    fun minusLids() {
        val current = inventory ?: return

        inventory = current.copy(
            lids = decrement(current.lids)
        )
    }

    fun plusCoffee() {
        val current = inventory ?: return
        val max = station?.settings?.coffeeMax ?: return

        inventory = current.copy(
            coffee = increment(
                current.coffee,
                max
            )
        )
    }

    fun minusCoffee() {
        val current = inventory ?: return

        inventory = current.copy(
            coffee = decrement(current.coffee)
        )
    }

    fun plusMilk() {
        val current = inventory ?: return
        val max = station?.settings?.milkMax ?: return

        inventory = current.copy(
            milk = increment(
                current.milk,
                max
            )
        )
    }

    fun minusMilk() {
        val current = inventory ?: return

        inventory = current.copy(
            milk = decrement(current.milk)
        )
    }

    fun plusChocolate() {
        val current = inventory ?: return
        val max = station?.settings?.chocolateMax ?: return

        inventory = current.copy(
            chocolate = increment(
                current.chocolate,
                max
            )
        )
    }

    fun minusChocolate() {
        val current = inventory ?: return

        inventory = current.copy(
            chocolate = decrement(current.chocolate)
        )
    }

    fun plusTopping() {
        val current = inventory ?: return
        val max = station?.settings?.toppingMax ?: return

        inventory = current.copy(
            topping = increment(
                current.topping,
                max
            )
        )
    }

    fun minusTopping() {
        val current = inventory ?: return

        inventory = current.copy(
            topping = decrement(current.topping)
        )
    }

    fun plusSugar() {
        val current = inventory ?: return
        val max = station?.settings?.sugarMax ?: return

        inventory = current.copy(
            sugar = increment(
                current.sugar,
                max
            )
        )
    }

    fun minusSugar() {
        val current = inventory ?: return

        inventory = current.copy(
            sugar = decrement(current.sugar)
        )
    }

    fun plusStirrers() {
        val current = inventory ?: return
        val max = station?.settings?.stirrersMax ?: return

        inventory = current.copy(
            stirrers = increment(
                current.stirrers,
                max
            )
        )
    }

    fun minusStirrers() {
        val current = inventory ?: return

        inventory = current.copy(
            stirrers = decrement(current.stirrers)
        )
    }

    fun plusGarbageBags() {
        val current = inventory ?: return
        val max = station?.settings?.garbageBagsMax ?: return

        inventory = current.copy(
            garbageBags = increment(
                current.garbageBags,
                max
            )
        )
    }

    fun minusGarbageBags() {
        val current = inventory ?: return

        inventory = current.copy(
            garbageBags = decrement(current.garbageBags)
        )
    }

    fun requestSave(
        isSupplyServiceFlow: Boolean,
        syncImmediately: Boolean = false,
        onSaved: () -> Unit = {}
    ) {
        if (isSaving) return

        val writeOffItems = if (isSupplyServiceFlow) {
            calculateWarehouseWriteOffItems()
        } else {
            emptyList()
        }

        if (writeOffItems.isNotEmpty()) {
            viewModelScope.launch {
                val writeOffItemsWithAvailability = attachWarehouseAvailability(writeOffItems)

                pendingWarehouseWriteOffItems = writeOffItemsWithAvailability
                warehouseWriteOffItems = writeOffItemsWithAvailability
                warehouseWriteOffApplied = false
                pendingWriteOffSource = WarehouseWriteOffSource.Warehouse
                selectedWriteOffSource = WarehouseWriteOffSource.Warehouse
                warehouseWriteOffErrorMessage = ""
                showWarehouseWriteOffConfirm = true
            }
            return
        }

        pendingWarehouseWriteOffItems = emptyList()
        warehouseWriteOffApplied = false
        pendingWriteOffSource = WarehouseWriteOffSource.Warehouse
        saveChanges(
            syncImmediately = syncImmediately,
            onSaved = onSaved
        )
    }

    fun confirmWarehouseWriteOff(
        syncImmediately: Boolean = false,
        onSaved: () -> Unit = {}
    ) {
        if (selectedWriteOffSource == WarehouseWriteOffSource.ExternalPurchase) {
            pendingWriteOffSource = WarehouseWriteOffSource.ExternalPurchase
            warehouseWriteOffErrorMessage = ""
            showWarehouseWriteOffConfirm = false

            saveChanges(
                syncImmediately = syncImmediately,
                onSaved = onSaved
            )
            return
        }

        if (warehouseWriteOffItems.any { item -> item.amount > item.available }) {
            warehouseWriteOffErrorMessage =
                "\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u043e\u0441\u0442\u0430\u0442\u043a\u043e\u0432 \u043d\u0430 \u0441\u043a\u043b\u0430\u0434\u0435"
            return
        }

        viewModelScope.launch {
            val shortages = warehouseRepository.getShortages(
                pendingWarehouseWriteOffItems.map { item ->
                    WarehouseStockWriteOff(
                        itemType = item.itemType,
                        amount = item.amount
                    )
                }
            )

            if (shortages.isNotEmpty()) {
                warehouseWriteOffErrorMessage = buildShortageMessage(shortages)
                return@launch
            }

            pendingWriteOffSource = WarehouseWriteOffSource.Warehouse
            warehouseWriteOffErrorMessage = ""
            showWarehouseWriteOffConfirm = false

            saveChanges(
                syncImmediately = syncImmediately,
                onSaved = onSaved
            )
        }
    }

    fun dismissWarehouseWriteOffConfirm() {
        showWarehouseWriteOffConfirm = false
    }

    fun selectWriteOffSource(source: WarehouseWriteOffSource) {
        selectedWriteOffSource = source
        warehouseWriteOffErrorMessage = ""
    }

    fun retrySave(
        syncImmediately: Boolean = false,
        onSaved: () -> Unit = {}
    ) {
        saveChanges(
            syncImmediately = syncImmediately,
            onSaved = onSaved
        )
    }

    fun saveChanges(
        syncImmediately: Boolean = false,
        onSaved: () -> Unit = {}
    ) {
        if (isSaving) return

        val detail = station ?: return
        val currentInventory = inventory ?: return

        viewModelScope.launch {
            isSaving = true
            showSyncError = false

            val stationSaved = repository.saveStation(
                detail = detail.copy(
                    inventory = currentInventory
                ),
                syncImmediately = syncImmediately
            )

            var warehouseSaved = true
            var warehouseErrorMessage = DEFAULT_SYNC_ERROR_MESSAGE

            if (pendingWarehouseWriteOffItems.isNotEmpty() &&
                pendingWriteOffSource == WarehouseWriteOffSource.Warehouse
            ) {
                val warehouseResult = if (warehouseWriteOffApplied) {
                    warehouseRepository.syncCurrentNow()
                } else {
                    warehouseWriteOffApplied = true
                    warehouseRepository.applyWriteOffNow(
                        pendingWarehouseWriteOffItems.map { item ->
                            WarehouseStockWriteOff(
                                itemType = item.itemType,
                                amount = item.amount
                            )
                        }
                    )
                }

                warehouseSaved = warehouseResult.synced
                warehouseErrorMessage = GoogleSheetsSyncMessages.errorMessage(
                    warehouseResult.errorType
                )
            }

            val updatedStation = repository.getStation(detail.station.id)
            station = updatedStation
            inventory = updatedStation?.inventory?.copy() ?: currentInventory
            isSaving = false

            if (stationSaved && warehouseSaved) {
                pendingWarehouseWriteOffItems = emptyList()
                warehouseWriteOffItems = emptyList()
                warehouseWriteOffApplied = false
                pendingWriteOffSource = WarehouseWriteOffSource.Warehouse
                onSaved()
            } else {
                syncErrorMessage = if (!stationSaved) {
                    station?.syncErrorDialogMessage() ?: DEFAULT_SYNC_ERROR_MESSAGE
                } else {
                    warehouseErrorMessage
                }
                showSyncError = true
            }
        }
    }

    fun dismissSyncError() {
        showSyncError = false
    }

    private fun calculateWarehouseWriteOffItems(): List<WarehouseWriteOffUiItem> {
        val original = station?.inventory ?: return emptyList()
        val current = inventory ?: return emptyList()

        return listOf(
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Cups.title,
                itemType = WarehouseItemType.Cups.code,
                iconRes = WarehouseItemType.Cups.iconRes,
                amount = positiveDelta(
                    original.cups,
                    current.cups
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Lids.title,
                itemType = WarehouseItemType.Lids.code,
                iconRes = WarehouseItemType.Lids.iconRes,
                amount = positiveDelta(
                    original.lids,
                    current.lids
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Sugar.title,
                itemType = WarehouseItemType.Sugar.code,
                iconRes = WarehouseItemType.Sugar.iconRes,
                amount = positiveDelta(
                    original.sugar,
                    current.sugar
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Stirrers.title,
                itemType = WarehouseItemType.Stirrers.code,
                iconRes = WarehouseItemType.Stirrers.iconRes,
                amount = positiveDelta(
                    original.stirrers,
                    current.stirrers
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.GarbageBags.title,
                itemType = WarehouseItemType.GarbageBags.code,
                iconRes = WarehouseItemType.GarbageBags.iconRes,
                amount = positiveDelta(
                    original.garbageBags,
                    current.garbageBags
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Coffee.title,
                itemType = WarehouseItemType.Coffee.code,
                iconRes = WarehouseItemType.Coffee.iconRes,
                amount = positiveDelta(
                    original.coffee,
                    current.coffee
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Milk.title,
                itemType = WarehouseItemType.Milk.code,
                iconRes = WarehouseItemType.Milk.iconRes,
                amount = positiveDelta(
                    original.milk,
                    current.milk
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Chocolate.title,
                itemType = WarehouseItemType.Chocolate.code,
                iconRes = WarehouseItemType.Chocolate.iconRes,
                amount = positiveDelta(
                    original.chocolate,
                    current.chocolate
                )
            ),
            WarehouseWriteOffUiItem(
                title = WarehouseItemType.Topping.title,
                itemType = WarehouseItemType.Topping.code,
                iconRes = WarehouseItemType.Topping.iconRes,
                amount = positiveDelta(
                    original.topping,
                    current.topping
                )
            )
        ).filter { item -> item.amount > 0 }
    }

    private suspend fun attachWarehouseAvailability(
        items: List<WarehouseWriteOffUiItem>
    ): List<WarehouseWriteOffUiItem> {
        val quantityByType = warehouseRepository.getQuantityByType()

        return items.map { item ->
            item.copy(
                available = quantityByType[item.itemType] ?: 0
            )
        }
    }

    private fun positiveDelta(
        original: Int,
        current: Int
    ): Int {
        return (current - original).coerceAtLeast(0)
    }

    private fun increment(
        current: Int,
        max: Int
    ): Int {
        return (current + 1)
            .coerceAtMost(max)
    }

    private fun decrement(
        current: Int
    ): Int {
        return (current - 1)
            .coerceAtLeast(0)
    }

    private fun buildShortageMessage(
        shortages: List<ru.coffeepoint.core.data.repository.WarehouseStockShortage>
    ): String {
        val titleByType = warehouseWriteOffItems
            .associate { item -> item.itemType to item.title }

        return shortages.joinToString(
            separator = "\n",
            prefix = "\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u043e\u0441\u0442\u0430\u0442\u043a\u043e\u0432 \u043d\u0430 \u0441\u043a\u043b\u0430\u0434\u0435:\n"
        ) { shortage ->
            val title = titleByType[shortage.itemType] ?: shortage.itemType

            "$title: \u043d\u0443\u0436\u043d\u043e ${shortage.required}, \u043d\u0430 \u0441\u043a\u043b\u0430\u0434\u0435 ${shortage.available}"
        }
    }

    private companion object {
        const val DEFAULT_SYNC_ERROR_MESSAGE =
            "\u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0443 \u0438\u043b\u0438 VPN"
    }
}

data class WarehouseWriteOffUiItem(
    val title: String,
    val itemType: String,
    val iconRes: Int,
    val amount: Int,
    val available: Int = 0
)

enum class WarehouseWriteOffSource(
    val title: String
) {
    Warehouse(
        title = "\u0421\u043a\u043b\u0430\u0434"
    ),
    ExternalPurchase(
        title = "\u0421\u0442\u043e\u0440\u043e\u043d\u043d\u044f\u044f \u0437\u0430\u043a\u0443\u043f\u043a\u0430"
    )
}
