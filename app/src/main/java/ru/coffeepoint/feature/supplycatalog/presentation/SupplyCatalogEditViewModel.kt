package ru.coffeepoint.feature.supplycatalog.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.repository.SupplyCatalogRepository
import ru.coffeepoint.feature.supplycatalog.model.SupplyCatalogItemType

class SupplyCatalogEditViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = SupplyCatalogRepository(application)
    private var loadedItem: SupplyCatalogEntity? = null
    private var loadedItemId: Int? = null
    private var pendingAction: PendingAction? = null

    var loading by mutableStateOf(false)
        private set

    var isSending by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    var showDeleteConfirm by mutableStateOf(false)
        private set

    var itemType by mutableStateOf("")
        private set

    var title by mutableStateOf("")
        private set

    var supplier by mutableStateOf("")
        private set

    var article by mutableStateOf("")
        private set

    var url by mutableStateOf("")
        private set

    var maxPrice by mutableStateOf("")
        private set

    var isActive by mutableStateOf(true)
        private set

    var priority by mutableStateOf("")
        private set

    var comment by mutableStateOf("")
        private set

    var titleError by mutableStateOf(false)
        private set

    var supplierError by mutableStateOf(false)
        private set

    var maxPriceError by mutableStateOf(false)
        private set

    fun loadItem(
        itemId: Int,
        initialItemType: String
    ) {
        if (loadedItemId == itemId) return

        loadedItemId = itemId

        viewModelScope.launch {
            loading = true

            if (itemId == 0) {
                val itemTypeCode = SupplyCatalogItemType.fromCode(initialItemType).code
                loadedItem = SupplyCatalogEntity(
                    id = 0,
                    itemType = itemTypeCode,
                    title = "",
                    supplier = "",
                    article = "",
                    url = "",
                    maxPrice = 0,
                    isActive = true,
                    priority = 1,
                    comment = ""
                )
                itemType = itemTypeCode
                title = ""
                supplier = ""
                article = ""
                url = ""
                maxPrice = ""
                isActive = true
                priority = "1"
                comment = ""
            } else {
                repository.getItem(itemId)?.let { item ->
                    loadedItem = item
                    itemType = item.itemType
                    title = item.title
                    supplier = item.supplier
                    article = item.article
                    url = item.url
                    maxPrice = item.maxPrice.toString()
                    isActive = item.isActive
                    priority = item.priority.toString()
                    comment = item.comment
                }
            }

            loading = false
        }
    }

    fun onItemTypeChanged(value: SupplyCatalogItemType) {
        itemType = value.code
    }

    fun onTitleChanged(value: String) {
        title = value
        titleError = false
    }

    fun onSupplierChanged(value: String) {
        supplier = value
        supplierError = false
    }

    fun onArticleChanged(value: String) {
        article = value
    }

    fun onUrlChanged(value: String) {
        url = value
    }

    fun onMaxPriceChanged(value: String) {
        maxPrice = value.filter { char -> char.isDigit() }
        maxPriceError = false
    }

    fun onIsActiveChanged(value: Boolean) {
        isActive = value
    }

    fun onPriorityChanged(value: String) {
        priority = value.filter { char -> char.isDigit() }
    }

    fun onCommentChanged(value: String) {
        comment = value
    }

    fun save(onSaved: () -> Unit) {
        val item = validateAndBuildItem() ?: return
        pendingAction = PendingAction.Upsert(item)
        sendPendingAction(onSaved)
    }

    fun requestDelete() {
        showDeleteConfirm = true
    }

    fun dismissDeleteConfirm() {
        showDeleteConfirm = false
    }

    fun delete(onDeleted: () -> Unit) {
        val source = loadedItem ?: return
        if (source.id == 0) return

        showDeleteConfirm = false
        pendingAction = PendingAction.Delete(source.id)
        sendPendingAction(onDeleted)
    }

    fun retry(onSuccess: () -> Unit) {
        sendPendingAction(onSuccess)
    }

    fun dismissSyncError() {
        showSyncError = false
    }

    private fun validateAndBuildItem(): SupplyCatalogEntity? {
        val source = loadedItem ?: return null
        var hasError = false

        if (title.isBlank()) {
            titleError = true
            hasError = true
        }

        if (supplier.isBlank()) {
            supplierError = true
            hasError = true
        }

        if (maxPrice.isBlank()) {
            maxPriceError = true
            hasError = true
        }

        if (hasError) return null

        return source.copy(
            itemType = SupplyCatalogItemType.fromCode(itemType).code,
            title = title.trim(),
            supplier = supplier.trim(),
            article = article.trim(),
            url = url.trim(),
            maxPrice = maxPrice.toIntOrNull() ?: 0,
            isActive = isActive,
            priority = priority.toIntOrNull() ?: 1,
            comment = comment.trim()
        )
    }

    private fun sendPendingAction(onSuccess: () -> Unit) {
        val action = pendingAction ?: return

        viewModelScope.launch {
            isSending = true
            showSyncError = false

            val result = when (action) {
                is PendingAction.Upsert -> {
                    val item = if (action.item.id == 0) {
                        action.item.copy(
                            id = repository.getNextItemId()
                        )
                    } else {
                        action.item
                    }

                    pendingAction = PendingAction.Upsert(item)
                    loadedItem = item

                    repository.upsertItemNow(item)
                }

                is PendingAction.Delete -> {
                    repository.deleteItemNow(action.itemId)
                }
            }

            isSending = false

            if (result.synced) {
                pendingAction = null
                onSuccess()
            } else {
                syncErrorMessage = repository.syncErrorMessage(result.errorType)
                showSyncError = true
            }
        }
    }

    private sealed interface PendingAction {
        data class Upsert(
            val item: SupplyCatalogEntity
        ) : PendingAction

        data class Delete(
            val itemId: Int
        ) : PendingAction
    }

}
