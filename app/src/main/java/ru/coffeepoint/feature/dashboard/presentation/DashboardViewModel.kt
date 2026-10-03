package ru.coffeepoint.feature.dashboard.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.repository.SalesRefreshRepository
import ru.coffeepoint.core.data.repository.SalesRefreshResult
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.SupplyCatalogRepository
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.sync.google.GoogleSheetsImportResult
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)
    private val salesRefreshRepository = SalesRefreshRepository(application)
    private val warehouseStockRepository = WarehouseStockRepository(application)
    private val supplyCatalogRepository = SupplyCatalogRepository(application)

    var stations by mutableStateOf<List<StationDetail>>(emptyList())
        private set

    var warehouseStock by mutableStateOf<List<WarehouseStockItem>>(emptyList())
        private set

    var supplyCatalogItems by mutableStateOf<List<SupplyCatalogEntity>>(emptyList())
        private set

    var showConnectionError by mutableStateOf(false)
        private set

    var showGoogleSheetsSyncError by mutableStateOf(false)
        private set

    var showSalesRefreshSuccess by mutableStateOf(false)
        private set

    var isRefreshingSales by mutableStateOf(false)
        private set

    var missingTerminalErrorText by mutableStateOf<String?>(null)
        private set

    var lastSalesRefreshText by mutableStateOf<String?>(null)
        private set

    fun loadStations() {

        viewModelScope.launch {

            stations = repository.getStations()
            warehouseStock = warehouseStockRepository.getItems()
            supplyCatalogItems = supplyCatalogRepository.getItems()
            lastSalesRefreshText =
                salesRefreshRepository.getLastRefreshText()

            val imported = repository.importFromGoogleSheets()

            if (imported) {
                stations = repository.getStations()
                warehouseStock = warehouseStockRepository.getItems()
                supplyCatalogItems = supplyCatalogRepository.getItems()
            }

        }

    }

    fun refreshSales() {

        if (isRefreshingSales) return

        viewModelScope.launch {

            isRefreshingSales = true
            missingTerminalErrorText = null
            showSalesRefreshSuccess = false

            try {

                when (importFromGoogleSheetsWithPendingSync()) {

                    GoogleSheetsImportResult.Success -> {
                        stations = repository.getStations()
                        warehouseStock = warehouseStockRepository.getItems()
                        supplyCatalogItems = supplyCatalogRepository.getItems()
                    }

                    GoogleSheetsImportResult.SkippedPendingChanges,
                    GoogleSheetsImportResult.Empty,
                    GoogleSheetsImportResult.Error -> {
                        showGoogleSheetsSyncError = true
                        return@launch
                    }

                }

                refreshVendistaSales(
                    showSuccess = true
                )

            } finally {
                isRefreshingSales = false
            }

        }

    }

    private suspend fun importFromGoogleSheetsWithPendingSync(): GoogleSheetsImportResult {

        val firstResult = repository.importFromGoogleSheetsResult()

        if (firstResult != GoogleSheetsImportResult.SkippedPendingChanges) {
            return firstResult
        }

        repository.syncPendingGoogleSheets()
        warehouseStockRepository.syncPendingGoogleSheets()

        repository.resolveConflictBlocksForImport()

        return repository.importFromGoogleSheetsResult()

    }

    fun refreshSalesWithoutGoogleSheets() {

        if (isRefreshingSales) return

        viewModelScope.launch {

            isRefreshingSales = true
            showGoogleSheetsSyncError = false
            missingTerminalErrorText = null
            showSalesRefreshSuccess = false

            try {
                refreshVendistaSales(
                    showSuccess = false
                )
            } finally {
                isRefreshingSales = false
            }

        }

    }

    private suspend fun refreshVendistaSales(
        showSuccess: Boolean
    ) {

        when (val result = salesRefreshRepository.refreshSales()) {

            is SalesRefreshResult.Success -> {

                if (result.missingTerminalNames.isNotEmpty()) {

                    missingTerminalErrorText =
                        "У станций ${result.missingTerminalNames.joinToString(", ")} — не указан номер терминала"

                }

                stations = repository.getStations()
                lastSalesRefreshText =
                    salesRefreshRepository.getLastRefreshText()
                showSalesRefreshSuccess =
                    showSuccess && result.missingTerminalNames.isEmpty()

            }

            SalesRefreshResult.NoInternet,
            SalesRefreshResult.ConnectionError -> {

                showConnectionError = true

            }

        }

    }

    fun dismissGoogleSheetsSyncError() {

        showGoogleSheetsSyncError = false

    }

    fun dismissSalesRefreshSuccess() {

        showSalesRefreshSuccess = false

    }

    fun dismissConnectionError() {

        showConnectionError = false

    }

    fun dismissMissingTerminalError() {

        missingTerminalErrorText = null

    }

}
