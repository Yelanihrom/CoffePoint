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

class SupplyCatalogViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = SupplyCatalogRepository(application)

    var items by mutableStateOf<List<SupplyCatalogEntity>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var showRefreshError by mutableStateOf(false)
        private set

    fun loadItems() {
        viewModelScope.launch {
            items = repository.getItems()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            showRefreshError = false

            val refreshed = repository.refreshFromGoogleSheets()

            if (refreshed) {
                items = repository.getItems()
            } else {
                showRefreshError = true
            }

            loading = false
        }
    }

    fun dismissRefreshError() {
        showRefreshError = false
    }

}
