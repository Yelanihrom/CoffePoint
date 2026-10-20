package ru.coffeepoint.feature.warehouse.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncMessages
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem

class WarehouseStockSettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WarehouseStockRepository(application)

    var items by mutableStateOf<List<WarehouseStockItem>>(emptyList())
        private set

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("")
        private set

    fun load() {
        if (items.isNotEmpty()) return

        viewModelScope.launch {
            items = repository.getItems()
        }
    }

    fun incrementMin(itemType: String) {
        updateItem(itemType) { item ->
            item.copy(
                minQuantity = item.minQuantity + 1
            )
        }
    }

    fun decrementMin(itemType: String) {
        updateItem(itemType) { item ->
            item.copy(
                minQuantity = (item.minQuantity - 1).coerceAtLeast(0)
            )
        }
    }

    fun updateMin(
        itemType: String,
        value: String
    ) {
        updateItem(itemType) { item ->
            item.copy(
                minQuantity = value.toIntOrNull()?.coerceAtLeast(0) ?: 0
            )
        }
    }

    fun incrementMax(itemType: String) {
        updateItem(itemType) { item ->
            item.copy(
                maxQuantity = item.maxQuantity + 1
            )
        }
    }

    fun decrementMax(itemType: String) {
        updateItem(itemType) { item ->
            item.copy(
                maxQuantity = (item.maxQuantity - 1).coerceAtLeast(0)
            )
        }
    }

    fun updateMax(
        itemType: String,
        value: String
    ) {
        updateItem(itemType) { item ->
            item.copy(
                maxQuantity = value.toIntOrNull()?.coerceAtLeast(0) ?: 0
            )
        }
    }

    fun save(onSaved: () -> Unit) {
        if (isSaving) return

        viewModelScope.launch {
            isSaving = true
            showSyncError = false

            val normalizedItems = items.map { item ->
                if (item.maxQuantity > 0 && item.minQuantity > item.maxQuantity) {
                    item.copy(
                        minQuantity = item.maxQuantity
                    )
                } else {
                    item
                }
            }

            items = normalizedItems

            val result = repository.saveItemsNow(normalizedItems)

            isSaving = false
            items = repository.getItems()

            if (result.synced) {
                onSaved()
            } else {
                syncErrorMessage = syncErrorMessage(result.errorType)
                showSyncError = true
            }
        }
    }

    fun retry(onSaved: () -> Unit) {
        save(onSaved)
    }

    fun dismissSyncError() {
        showSyncError = false
    }

    private fun updateItem(
        itemType: String,
        transform: (WarehouseStockItem) -> WarehouseStockItem
    ) {
        items = items.map { item ->
            if (item.itemType == itemType) {
                transform(item)
            } else {
                item
            }
        }
    }

    private fun syncErrorMessage(errorType: GoogleSheetsSyncErrorType): String {
        return GoogleSheetsSyncMessages.errorMessage(errorType)
    }

}
