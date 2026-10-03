package ru.coffeepoint.feature.warehouse.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.repository.SupplyCatalogRepository
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncMessages
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem

class WarehouseStockViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WarehouseStockRepository(application)
    private val supplyCatalogRepository = SupplyCatalogRepository(application)

    var items by mutableStateOf<List<WarehouseStockItem>>(emptyList())
        private set

    var editableItems by mutableStateOf<List<WarehouseStockItem>>(emptyList())
        private set

    var supplyCatalogItems by mutableStateOf<List<SupplyCatalogEntity>>(emptyList())
        private set

    var isEditing by mutableStateOf(false)
        private set

    var isSending by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("")
        private set

    private var pendingItems: List<WarehouseStockItem>? = null

    fun load() {
        viewModelScope.launch {
            items = repository.getItems()
            supplyCatalogItems = supplyCatalogRepository.getItems()
            if (!isEditing) {
                editableItems = items
            }
        }
    }

    fun startEditing() {
        editableItems = items
        isEditing = true
    }

    fun cancelEditing() {
        editableItems = items
        isEditing = false
    }

    fun increment(itemType: String) {
        updateEditableQuantity(itemType) { quantity -> quantity + 1 }
    }

    fun decrement(itemType: String) {
        updateEditableQuantity(itemType) { quantity -> (quantity - 1).coerceAtLeast(0) }
    }

    fun save(onSaved: () -> Unit) {
        pendingItems = editableItems
        sendPendingItems(onSaved)
    }

    fun retry(onSaved: () -> Unit) {
        sendPendingItems(onSaved)
    }

    fun dismissSyncError() {
        showSyncError = false
    }

    private fun sendPendingItems(onSaved: () -> Unit) {
        val itemsToSave = pendingItems ?: return

        viewModelScope.launch {
            isSending = true
            showSyncError = false

            val result = repository.saveItemsNow(itemsToSave)

            isSending = false
            items = repository.getItems()
            editableItems = items

            if (result.synced) {
                pendingItems = null
                isEditing = false
                onSaved()
            } else {
                syncErrorMessage = syncErrorMessage(result.errorType)
                showSyncError = true
            }
        }
    }

    private fun syncErrorMessage(errorType: GoogleSheetsSyncErrorType): String {
        return GoogleSheetsSyncMessages.errorMessage(errorType)
    }

    private fun updateEditableQuantity(
        itemType: String,
        transform: (Int) -> Int
    ) {
        editableItems = editableItems.map { item ->
            if (item.itemType == itemType) {
                item.copy(quantity = transform(item.quantity))
            } else {
                item
            }
        }
    }
}
